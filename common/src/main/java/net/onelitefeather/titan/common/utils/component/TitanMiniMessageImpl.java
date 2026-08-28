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
package net.onelitefeather.titan.common.utils.component;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TitanMiniMessageImpl implements MiniMessage.Provider {
    @Override
    public @NotNull MiniMessage miniMessage() {
        return MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard(), Placeholder.component("prefix", TitanMiniMessageImpl::prefix))).build();
    }

    private static @NotNull Component prefix() {
        return MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard())).build().deserialize("<gradient:#00ddff:#ffffff>Titan</gradient>");
    }

    @Override
    public @NotNull Consumer<MiniMessage.Builder> builder() {
        return builder -> {
        };
    }
}
