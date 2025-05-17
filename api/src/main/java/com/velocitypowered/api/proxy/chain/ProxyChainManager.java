package com.velocitypowered.api.proxy.chain;

import com.velocitypowered.api.proxy.Player;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manages connections between Velocity proxies.
 */
public interface ProxyChainManager {

  /** Returns all configured proxies keyed by name. */
  Map<String, InetSocketAddress> getProxies();

  /** Retrieves a proxy by name. */
  Optional<InetSocketAddress> getProxy(String name);

  /** Registers a proxy at runtime. */
  void registerProxy(String name, InetSocketAddress address);

  /** Unregisters a proxy. */
  void unregisterProxy(String name);

  /**
   * Connects a player to another proxy.
   *
   * @param player the player
   * @param proxyName the destination proxy
   * @return future indicating success
   */
  CompletableFuture<Boolean> connectPlayer(Player player, String proxyName);
}

