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

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.onelitefeather.titan.common.season.SeasonPrefix;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TitanMiniMessageImpl implements MiniMessage.Provider {
    @Override
    public @NotNull MiniMessage miniMessage() {
        return MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard(), prefixResolver())).build();
    }

    /**
     * Resolves {@code <prefix>} when the tag is used rather than when this resolver is built.
     *
     * <p>Adventure caches the {@link MiniMessage} instance a provider returns, and
     * {@code Placeholder.component} resolves its value eagerly - so a placeholder built here would
     * freeze the prefix at the first message the process ever sends. A running season needs it to
     * be read every time; see {@link SeasonPrefix}.
     */
    private static @NotNull TagResolver prefixResolver() {
        return TagResolver.resolver("prefix", (arguments, context) -> Tag.selfClosingInserting(SeasonPrefix.current()));
    }

    @Override
    public @NotNull Consumer<MiniMessage.Builder> builder() {
        return builder -> {
        };
    }
}
