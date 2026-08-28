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
        final Material registryValue = Material.values().stream().filter(material -> material.key().asString().equalsIgnoreCase(input)).findAny().orElseThrow(() -> new ArgumentSyntaxException("Registry value is invalid", input, INVALID_NAME));
        return registryValue.key().asString();
    }
}
