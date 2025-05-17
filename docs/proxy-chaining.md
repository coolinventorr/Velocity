# Proxy Chaining with Velocity

This document describes how to chain multiple Velocity proxies together using the built-in proxy
chaining support.

## Configuration

Create `proxychain.toml` in your Velocity directory (generated automatically on first run). Define
each remote proxy under the `[proxies]` section. The key is the name and the value is the
`host:port` of the next proxy.

Example:
```toml
[proxies]
lobby2 = "127.0.0.1:25566"
```

Ensure all proxies in the chain share the same `player-info-forwarding-mode` and `forwarding-secret` in
`velocity.toml`.

## Usage

Use `/chain <proxyName>` in-game to move to another configured proxy. All proxies listed are
registered automatically on startup and will authenticate using the shared secret.
