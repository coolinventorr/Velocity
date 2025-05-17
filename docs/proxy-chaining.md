# Proxy Chaining with Velocity

Velocity can forward players between multiple proxy instances. Configuration is stored in `proxychain.toml` next to `velocity.toml`.

## Configuration

1. On first run a `proxychain.toml` file is created using `default-proxychain.toml`.
2. Set a shared `secret` on all proxies.
3. List remote proxies under `[proxies]` using `name = "host:port"` pairs.

Example:
```toml
secret = "change_me"
[proxies]
lobby2 = "127.0.0.1:25566"
```

## Usage

Players can use `/chain <proxy>` to move between proxies. Plugins may access
`ProxyServer.getProxyChainManager()` to programmatically connect players.
The manager sends a short authentication token to verify transitions.

