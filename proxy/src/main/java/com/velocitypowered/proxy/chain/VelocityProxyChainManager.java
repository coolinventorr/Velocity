package com.velocitypowered.proxy.chain;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Default implementation of proxy chaining support.
 */
public class VelocityProxyChainManager implements com.velocitypowered.api.proxy.chain.ProxyChainManager {

  private static final MinecraftChannelIdentifier CHANNEL =
      MinecraftChannelIdentifier.create("proxychain", "auth");

  private final VelocityServer server;
  private String secret = "change_me";
  private final Map<String, RegisteredServer> proxies = new HashMap<>();
  private final Map<UUID, Boolean> authenticated = new HashMap<>();
  private final Path dataDirectory;

  @Inject
  public VelocityProxyChainManager(VelocityServer server, Path dataDirectory) {
    this.server = server;
    this.dataDirectory = dataDirectory;
  }

  /** Loads the proxy chain configuration and registers proxies. */
  public void loadConfig() {
    Path configPath = dataDirectory.resolve("proxychain.toml");
    try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
        .defaultData(VelocityProxyChainManager.class.getResource("/default-proxychain.toml"))
        .autosave()
        .sync()
        .build()) {
      config.load();
      this.secret = config.getOrElse("secret", "change_me");
      Map<String, String> confProxies = config.get("proxies");
      if (confProxies != null) {
        for (Map.Entry<String, String> e : confProxies.entrySet()) {
          String[] parts = e.getValue().split(":", 2);
          if (parts.length != 2) continue;
          try {
            int port = Integer.parseInt(parts[1]);
            registerProxy(e.getKey(), new InetSocketAddress(parts[0], port));
          } catch (NumberFormatException ignore) {
          }
        }
      }
    }
  }

  @Override
  public void registerProxy(String name, InetSocketAddress address) {
    ServerInfo info = new ServerInfo(name, address);
    RegisteredServer rs = server.registerServer(info);
    proxies.put(name, rs);
  }

  @Override
  public Optional<RegisteredServer> getProxy(String name) {
    return Optional.ofNullable(proxies.get(name));
  }

  @Override
  public Map<String, RegisteredServer> getAllProxies() {
    return Collections.unmodifiableMap(proxies);
  }

  @Override
  public void connect(Player player, String proxyName) {
    Optional<RegisteredServer> target = getProxy(proxyName);
    if (target.isEmpty()) {
      player.sendMessage(Component.text("Unknown proxy: " + proxyName));
      return;
    }
    player.createConnectionRequest(target.get()).connect().whenComplete((res, ex) -> {
      if (ex != null || res == null || res.getStatus() != com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status.SUCCESS) {
        player.sendMessage(Component.text("Failed to connect"));
        return;
      }
      byte[] token = generateToken(player.getUniqueId());
      player.sendPluginMessage(CHANNEL, token);
    });
  }

  private byte[] generateToken(UUID uuid) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(secret.getBytes());
      digest.update(uuid.toString().getBytes());
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      return uuid.toString().getBytes();
    }
  }

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    authenticated.put(event.getPlayer().getUniqueId(), Boolean.FALSE);
    server.getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, () -> {
      if (!Boolean.TRUE.equals(authenticated.get(event.getPlayer().getUniqueId()))) {
        event.getPlayer().disconnect(Component.text("Proxy chain authentication failed"));
      }
      authenticated.remove(event.getPlayer().getUniqueId());
    }).delay(20L).schedule();
  }

  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    if (!event.getIdentifier().equals(CHANNEL)) {
      return;
    }
    if (!(event.getSource() instanceof Player player)) {
      return;
    }
    byte[] expected = generateToken(player.getUniqueId());
    if (MessageDigest.isEqual(expected, event.getData())) {
      authenticated.put(player.getUniqueId(), Boolean.TRUE);
    }
  }
}
