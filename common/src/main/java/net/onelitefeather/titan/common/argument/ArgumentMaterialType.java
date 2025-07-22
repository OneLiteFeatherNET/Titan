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
package net.onelitefeather.titan.common.argument;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentRegistry.INVALID_NAME;

public final class ArgumentMaterialType extends ArgumentString {

	public ArgumentMaterialType(@NotNull String id) {
		super(id);
	}

	@Override
	public @NotNull String parse(@NotNull CommandSender sender, @NotNull String input) throws ArgumentSyntaxException {
		final Material registryValue = Material.values().stream()
				.filter(material -> material.key().asString().equalsIgnoreCase(input)).findAny()
				.orElseThrow(() -> new ArgumentSyntaxException("Registry value is invalid", input, INVALID_NAME));
		return registryValue.key().asString();
	}
}
