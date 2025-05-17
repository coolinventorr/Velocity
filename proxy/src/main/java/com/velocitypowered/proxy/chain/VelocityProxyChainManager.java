package com.velocitypowered.proxy.chain;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.chain.ProxyChainManager;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link ProxyChainManager} built into the proxy.
 */
public class VelocityProxyChainManager implements ProxyChainManager {

  private static final String CHANNEL = "velocity:proxychain";
  private final VelocityServer server;
  private final Map<String, InetSocketAddress> proxies = new HashMap<>();
  private String secret = "";

  public VelocityProxyChainManager(VelocityServer server) {
    this.server = server;
    loadConfig();
  }

  private void loadConfig() {
    Path configPath = Path.of("proxychain.toml");
    try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
        .defaultData(VelocityServer.class.getResource("/default-proxychain.toml"))
        .autosave()
        .sync()
        .build()) {
      config.load();
      secret = config.getOrElse("secret", "changeme");
      Map<String, String> map = config.get("proxies");
      if (map != null) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
          String[] parts = entry.getValue().split(":", 2);
          if (parts.length != 2) continue;
          try {
            int port = Integer.parseInt(parts[1]);
            registerProxy(entry.getKey(), new InetSocketAddress(parts[0], port));
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }
  }

  @Override
  public Map<String, InetSocketAddress> getProxies() {
    return Collections.unmodifiableMap(proxies);
  }

  @Override
  public Optional<InetSocketAddress> getProxy(String name) {
    return Optional.ofNullable(proxies.get(name));
  }

  @Override
  public void registerProxy(String name, InetSocketAddress address) {
    proxies.put(name, address);
    ServerInfo info = new ServerInfo(name, address);
    server.registerServer(info);
  }

  @Override
  public void unregisterProxy(String name) {
    InetSocketAddress addr = proxies.remove(name);
    if (addr != null) {
      server.unregisterServer(new ServerInfo(name, addr));
    }
  }

  @Override
  public CompletableFuture<Boolean> connectPlayer(Player player, String proxyName) {
    Optional<RegisteredServer> rs = server.getServer(proxyName);
    if (rs.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    return player.createConnectionRequest(rs.get()).connect().thenApply(success -> {
      if (success) {
        player.getConnectedServer().sendPluginMessage(
            () -> CHANNEL,
            secret.getBytes()
        );
      }
      return success;
    });
  }
}

