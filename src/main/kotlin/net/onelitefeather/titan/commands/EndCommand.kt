package net.onelitefeather.titan.commands

import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import kotlin.system.exitProcess

class EndCommand : Command("end") {

    init {
        addSyntax(this::run)
        setCondition(this::hasPermission)
    }

    private fun hasPermission(commandSender: CommandSender, s: String?): Boolean {
        return commandSender.hasPermission("lobby.end");
    }

    private fun run(commandSender: CommandSender, context: CommandContext) {
        MinecraftServer.stopCleanly()
        exitProcess(0)
    }

}