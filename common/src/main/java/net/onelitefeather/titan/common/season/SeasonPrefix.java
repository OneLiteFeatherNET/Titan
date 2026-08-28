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
package net.onelitefeather.titan.common.season;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The component the {@code <prefix>} tag resolves to in every message the lobby writes.
 *
 * <p>A message prefix is the one seasonal effect that is neither in the world nor per player: it is
 * a value the whole server reads while rendering text. It therefore lives here, in one place, read
 * by {@code TitanMiniMessageImpl} and written by exactly one season at a time — the
 * highest-priority
 * one that sets it, since seasons are applied in ascending priority.
 *
 * <p>The default is the one Titan has always had, and a season that ends puts it back. That is the
 * same undo discipline as a block: what is restored is whatever was read before the change, so two
 * overlapping seasons unwind to the default rather than to each other's guesses.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class SeasonPrefix {

    /** The prefix outside any season: the Titan wordmark. */
    public static final Component DEFAULT = MiniMessage.builder().tags(TagResolver.standard()).build().deserialize("<gradient:#00ddff:#ffffff>Titan</gradient>");

    private static final AtomicReference<Component> CURRENT = new AtomicReference<>(DEFAULT);

    private SeasonPrefix() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Returns the prefix in force right now.
     *
     * @return the seasonal prefix, or {@link #DEFAULT} when no season sets one
     */
    @Contract(pure = true)
    public static Component current() {
        return CURRENT.get();
    }

    /**
     * Sets the prefix in force.
     *
     * @param prefix the prefix to use, or {@code null} to go back to {@link #DEFAULT}
     */
    public static void current(@Nullable Component prefix) {
        CURRENT.set(prefix == null ? DEFAULT : prefix);
    }
}
