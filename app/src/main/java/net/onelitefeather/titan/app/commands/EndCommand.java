/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.app.commands;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import org.jetbrains.annotations.Nullable;

public final class EndCommand extends Command {
    public EndCommand() {
        super("end");
        setCondition(this::hasPermission);
        addSyntax(this::execute);
    }

    private void execute(CommandSender commandSender, CommandContext commandContext) {
        MinecraftServer.stopCleanly();
        System.exit(0);
    }

    private boolean hasPermission(CommandSender commandSender, @Nullable String s) {
        return false;
        // return commandSender.hasPermission("titan.command.end") ||
        // commandSender.hasPermission("lobby.end");
    }
}
