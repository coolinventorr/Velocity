package com.example.proxychain;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Plugin(
    id = "proxychainexample",
    name = "ProxyChainExample",
    version = "1.0.0",
    description = "Example plugin demonstrating basic proxy chaining"
)
public class ProxyChainPlugin {
  private final ProxyServer server;
  private final Path dataDir;
  private String secret;
  private final Map<UUID, String> pendingTokens = new ConcurrentHashMap<>();
  private static final MinecraftChannelIdentifier AUTH_CHANNEL =
      MinecraftChannelIdentifier.create("proxychain", "auth");

  @Inject
  public ProxyChainPlugin(ProxyServer server, @DataDirectory Path dataDir) {
    this.server = server;
    this.dataDir = dataDir;
  }

  @Subscribe
  public void onInitialize(ProxyInitializeEvent event) {
    loadConfig();
    server.getCommandManager().register("chain", new ChainCommand());
    server.getChannelRegistrar().register(AUTH_CHANNEL);
    server.getEventManager().register(this, this);
  }

  private void loadConfig() {
    Path configPath = dataDir.resolve("proxychain.toml");
    try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
        .defaultData(ProxyChainPlugin.class.getResource("/proxychain.defaults.toml"))
        .autosave()
        .sync()
        .build()) {
      config.load();
      secret = config.getOrElse("secret", "change-me");
      Map<String, String> proxies = config.get("proxies");
      if (proxies != null) {
        for (Map.Entry<String, String> entry : proxies.entrySet()) {
          String name = entry.getKey();
          String[] parts = entry.getValue().split(":", 2);
          if (parts.length != 2) continue;
          int port;
          try {
            port = Integer.parseInt(parts[1]);
          } catch (NumberFormatException e) {
            continue;
          }
          server.registerServer(new ServerInfo(name, new InetSocketAddress(parts[0], port)));
        }
      }
    }
  }

  private String generateToken(UUID uuid) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      mac.update(uuid.toString().getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private class ChainCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
      if (!(invocation.source() instanceof Player)) {
        invocation.source().sendMessage(Component.text("Only players may use this command."));
        return;
      }
      Player player = (Player) invocation.source();
      if (invocation.arguments().length != 1) {
        player.sendMessage(Component.text("Usage: /chain <proxy>"));
        return;
      }
      String target = invocation.arguments()[0];
      Optional<RegisteredServer> rs = server.getServer(target);
      if (rs.isEmpty()) {
        player.sendMessage(Component.text("Unknown proxy: " + target));
        return;
      }
      String token = generateToken(player.getUniqueId());
      pendingTokens.put(player.getUniqueId(), token);
      player.createConnectionRequest(rs.get()).connect().whenComplete((res, ex) -> {
        if (ex != null) {
          player.sendMessage(Component.text("Failed to connect: " + ex.getMessage()));
        }
      });
    }
  }

  @Subscribe
  public void onServerConnected(ServerConnectedEvent event) {
    String token = pendingTokens.remove(event.getPlayer().getUniqueId());
    if (token != null) {
      event.getServer().sendPluginMessage(AUTH_CHANNEL, token.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    if (!event.getIdentifier().equals(AUTH_CHANNEL)) {
      return;
    }
    if (!(event.getSource() instanceof ServerConnection) || !(event.getTarget() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getTarget();
    String expected = generateToken(player.getUniqueId());
    String provided = new String(event.getData(), StandardCharsets.UTF_8);
    if (!provided.equals(expected)) {
      player.disconnect(Component.text("Invalid chain authentication."));
    }
    event.setResult(PluginMessageEvent.ForwardResult.denied());
  }
}
