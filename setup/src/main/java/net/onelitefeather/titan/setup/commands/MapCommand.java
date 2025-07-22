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
package net.onelitefeather.titan.setup.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class MapCommand extends Command {

	private static final Argument<String> MAP_NAME = ArgumentType.String("name");
	private static final Argument<String[]> MAP_AUTHOR = ArgumentType.StringArray("author");
	private final MapProvider mapProvider;

	public MapCommand(MapProvider mapProvider) {
		super("map");
		this.mapProvider = mapProvider;
		this.addSyntax(this::setSpawn, ArgumentType.Literal("setspawn"));
		this.addSyntax(this::setName, ArgumentType.Literal("setname"), MAP_NAME);
		this.addSyntax(this::setAuthor, ArgumentType.Literal("setauthor"), MAP_AUTHOR);
	}

	private void setAuthor(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
		var author = commandContext.get(MAP_AUTHOR);
		this.mapProvider.saveMap(LobbyMap.lobbyMapBuilder(this.mapProvider.getActiveLobby()).author(author).build());
		Component join = Component.join(JoinConfiguration.commas(false),
				Arrays.stream(author).map(Component::text).toList());
		commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> <green><name> authors set!",
				Placeholder.component("name", join)));
	}

	private void setName(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
		var name = commandContext.get(MAP_NAME);
		this.mapProvider.saveMap(LobbyMap.lobbyMapBuilder(this.mapProvider.getActiveLobby()).name(name).build());
		commandSender.sendMessage(
				MiniMessage.miniMessage().deserialize("<prefix> <green><name> set!", Placeholder.parsed("name", name)));
	}

	private void setSpawn(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
		if (commandSender instanceof Player player) {
			this.mapProvider.saveMap(
					LobbyMap.lobbyMapBuilder(this.mapProvider.getActiveLobby()).spawn(player.getPosition()).build());
			commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<prefix> <green>Spawn set!"));
		}
	}
}
