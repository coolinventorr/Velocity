# Proxy Chaining with Velocity

Velocity can forward players between multiple proxies. This feature is built in and configured through `proxychain.toml` located next to `velocity.toml`.

## Configuration

1. Create `proxychain.toml` if it does not exist. A default file is generated on first run.
2. Define each remote proxy under the `[proxies]` section as `name = "host:port"`.
3. Set `secret` to a shared token used to authenticate between proxies.

Example:
```toml
secret = "changeme"

[proxies]
lobby2 = "127.0.0.1:25566"
```

All proxies in the chain must share the same `player-info-forwarding-mode` and `forwarding-secret` in `velocity.toml`.

## Usage

Run `/chain <proxyName>` in game to move to the target proxy. Plugins can interact with chaining through `ProxyServer.getProxyChainManager()`.
