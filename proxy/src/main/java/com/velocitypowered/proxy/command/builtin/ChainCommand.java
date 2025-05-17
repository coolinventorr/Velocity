package com.velocitypowered.proxy.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

/**
 * Command allowing players to switch proxies using the chain manager.
 */
public class ChainCommand {
  private final ProxyServer server;

  public ChainCommand(ProxyServer server) {
    this.server = server;
  }

  public BrigadierCommand create() {
    LiteralCommandNode<CommandSource> node = BrigadierCommand
        .literalArgumentBuilder("chain")
        .requires(src -> src instanceof Player
            && src.getPermissionValue("velocity.command.chain") != Tristate.FALSE)
        .then(BrigadierCommand.requiredArgumentBuilder("proxy", StringArgumentType.word())
            .suggests((ctx, builder) -> {
              String arg = ctx.getArguments().containsKey("proxy")
                  ? StringArgumentType.getString(ctx, "proxy") : "";
              server.getProxyChainManager().getProxies().keySet().stream()
                  .filter(n -> n.regionMatches(true, 0, arg, 0, arg.length()))
                  .forEach(builder::suggest);
              return builder.buildFuture();
            })
            .executes(ctx -> {
              Player player = (Player) ctx.getSource();
              String target = StringArgumentType.getString(ctx, "proxy");
              server.getProxyChainManager().connectPlayer(player, target)
                  .thenAccept(success -> {
                    if (!success) {
                      player.sendMessage(Component.text("Failed to connect."));
                    }
                  });
              return Command.SINGLE_SUCCESS;
            }))
        .build();
    return new BrigadierCommand(node);
  }
}

