# Proxy Chaining with Velocity

This document describes a simple approach for chaining multiple Velocity proxies together. The included
`proxy-chain-example` plugin provides minimal functionality for forwarding players from one proxy to
another.

## Building the Plugin

Run the Gradle task `:proxy-chain-example:build` to compile the plugin. The resulting JAR can be found
in `proxy-chain-example/build/libs/`.

_Note: building requires network access to obtain dependencies._

## Configuration

1. Copy `proxychain.defaults.toml` from the plugin JAR to `plugins/proxychain.toml` (created
automatically on first run).
2. Set `secret` to the same value on all proxies. This value is used to authenticate players when
   chaining.
3. Define each remote proxy under the `[proxies]` section. The key is the name, and the value is the
   `host:port` of the next proxy.

Example:
```toml
secret = "change-me"

[proxies]
lobby2 = "127.0.0.1:25566"
```

Ensure all proxies in the chain share the same `player-info-forwarding-mode` and `forwarding-secret` in
`velocity.toml`.

## Usage

Install the plugin on every proxy in the chain. Use `/chain <proxyName>` in-game to move to the
specified proxy. The plugin will authenticate the jump using the shared secret and a plugin message
sent when the connection is established.
