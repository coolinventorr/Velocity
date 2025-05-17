package com.velocitypowered.proxy.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;

/**
 * Command used to forward players to another proxy in the chain.
 */
public class ChainCommand {
  private final ProxyServer server;

  public ChainCommand(ProxyServer server) {
    this.server = server;
  }

  public void register() {
    LiteralArgumentBuilder<CommandSource> root = BrigadierCommand
        .literalArgumentBuilder("chain")
        .requires(src -> src.getPermissionValue("velocity.command.chain") == Tristate.TRUE);

    RequiredArgumentBuilder<CommandSource, String> proxyArg = BrigadierCommand
        .requiredArgumentBuilder("proxy", StringArgumentType.word())
        .suggests((ctx, builder) -> {
          String arg = ctx.getArguments().containsKey("proxy")
              ? ctx.getArgument("proxy", String.class) : "";
          for (String name : server.getProxyChainManager().getAllProxies().keySet()) {
            if (name.regionMatches(true, 0, arg, 0, arg.length())) {
              builder.suggest(name);
            }
          }
          return builder.buildFuture();
        })
        .executes(ctx -> {
          if (!(ctx.getSource() instanceof Player)) {
            ctx.getSource().sendMessage(CommandMessages.PLAYERS_ONLY);
            return 0;
          }
          Player player = (Player) ctx.getSource();
          String target = ctx.getArgument("proxy", String.class);
          server.getProxyChainManager().connect(player, target);
          return Command.SINGLE_SUCCESS;
        });

    root.then(proxyArg);
    BrigadierCommand command = new BrigadierCommand(root);
    server.getCommandManager().register(
        server.getCommandManager().metaBuilder(command)
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        command);
  }
}
