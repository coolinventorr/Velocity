package com.velocitypowered.proxy.chain;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;

/**
 * Built-in manager providing simple proxy chaining support.
 */
public class ProxyChainManager {
  private static final MinecraftChannelIdentifier CHANNEL =
      MinecraftChannelIdentifier.create("proxychain", "auth");

  private final ProxyServer server;
  private final Path dataDir;
  private String secret = "";
  private final Map<String, String> remotes = new HashMap<>();

  @Inject
  public ProxyChainManager(ProxyServer server, @DataDirectory Path dataDir) {
    this.server = server;
    this.dataDir = dataDir;
  }

  @Subscribe
  public void onProxyInit(ProxyInitializeEvent event) {
    loadConfig();
    registerServers();
    registerCommand();
  }

  @Subscribe
  public void onServerConnected(ServerConnectedEvent event) {
    RegisteredServer rs = event.getServer();
    if (remotes.containsKey(rs.getServerInfo().getName())) {
      rs.sendPluginMessage(CHANNEL, secret.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    if (!event.getIdentifier().equals(CHANNEL)) {
      return;
    }
    if (event.getSource() instanceof Player player) {
      String received = new String(event.getData(), StandardCharsets.UTF_8);
      if (!secret.equals(received)) {
        player.disconnect(Component.text("Invalid proxy chain token"));
      }
      event.setResult(PluginMessageEvent.ForwardResult.handled());
    }
  }

  private void registerCommand() {
    server.getCommandManager().register(
        server.getCommandManager().metaBuilder("chain")
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        (invocation) -> {
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
          RegisteredServer rs = server.getServer(target);
          if (rs == null) {
            player.sendMessage(Component.text("Unknown proxy: " + target));
            return;
          }
          player.createConnectionRequest(rs).connect();
        });
  }

  private void registerServers() {
    for (Map.Entry<String, String> e : remotes.entrySet()) {
      String[] parts = e.getValue().split(":", 2);
      if (parts.length != 2) continue;
      try {
        int port = Integer.parseInt(parts[1]);
        server.registerServer(
            new com.velocitypowered.api.proxy.server.ServerInfo(e.getKey(),
                new java.net.InetSocketAddress(parts[0], port)));
      } catch (NumberFormatException ex) {
        // ignore invalid
      }
    }
  }

  private void loadConfig() {
    Path configPath = dataDir.resolve("proxychain.toml");
    try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
        .defaultData(ProxyChainManager.class.getResource("/default-proxychain.toml"))
        .autosave()
        .sync()
        .build()) {
      config.load();
      secret = config.getOrElse("secret", "change_me");
      Map<String, String> p = config.get("proxies");
      if (p != null) {
        remotes.clear();
        remotes.putAll(p);
      }
    }
  }

  public Set<String> getRemoteNames() {
    return Collections.unmodifiableSet(remotes.keySet());
  }
}
