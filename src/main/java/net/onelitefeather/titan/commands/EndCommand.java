package net.onelitefeather.titan.commands;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EndCommand extends Command {
    private EndCommand() {
        super("end");
        setCondition(this::hasPermission);
        addSyntax(this::execute);
    }

    private void execute(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        MinecraftServer.stopCleanly();
        System.exit(0);
    }

    private boolean hasPermission(@NotNull CommandSender commandSender, @Nullable String s) {
        return commandSender.hasPermission("titan.command.end") || commandSender.hasPermission("lobby.end");
    }

    public static EndCommand instance() {
        return new EndCommand();
    }
}
