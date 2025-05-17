package com.velocitypowered.api.proxy.chain;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;

/**
 * Provides facilities for chaining multiple Velocity proxies.
 */
public interface ProxyChainManager {

  /**
   * Registers a new proxy in the chain.
   *
   * @param name the proxy name
   * @param address the proxy address
   */
  void registerProxy(String name, InetSocketAddress address);

  /**
   * Retrieves a registered proxy by name.
   *
   * @param name the proxy name
   * @return the server representing the proxy, if registered
   */
  Optional<RegisteredServer> getProxy(String name);

  /**
   * Returns an immutable view of all registered proxies.
   *
   * @return the proxy map
   */
  Map<String, RegisteredServer> getAllProxies();

  /**
   * Connects the specified player to the named proxy.
   *
   * @param player the player
   * @param proxyName the target proxy name
   */
  void connect(Player player, String proxyName);
}
