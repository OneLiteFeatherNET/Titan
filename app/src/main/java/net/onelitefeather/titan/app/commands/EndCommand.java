/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.app.commands;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EndCommand extends Command {
	public EndCommand() {
		super("end");
		setCondition(this::hasPermission);
		addSyntax(this::execute);
	}

	private void execute(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
		MinecraftServer.stopCleanly();
		System.exit(0);
	}

	private boolean hasPermission(@NotNull CommandSender commandSender, @Nullable String s) {
		return false;
		// return commandSender.hasPermission("titan.command.end") ||
		// commandSender.hasPermission("lobby.end");
	}
}
