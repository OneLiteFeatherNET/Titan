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
package net.onelitefeather.titan.common.utils;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.tag.Tag;

import java.util.UUID;

/**
 * The {@link Tags} class contains all tags used in the project. It is a utility
 * class and should not be instantiated.
 *
 * @author themeinerlp
 * @version 2.0.0
 * @since 2.0.0
 **/
public final class Tags {

    public static final Tag<Long> TICKLE_COOLDOWN = Tag.Long("tickle_cooldown");
    public static final Tag<UUID> SIT_ARROW = Tag.UUID("SIT_ARROW");
    public static final Tag<Pos> SIT_PLAYER = Tag.Structure("SIT_PLAYER", Pos.class);

    /**
     * Id of the portal the player is currently standing in, or absent when they are standing in
     * none. A portal fires on the transition into this tag, not while it is set - that is what
     * keeps a player who stayed behind (a refused or failed switch) from triggering the portal
     * again on their next movement packet.
     *
     * <p>Transient: this is session state, and it must not survive into the player's NBT.
     */
    public static final Tag<String> PORTAL_INSIDE = Tag.Transient("portal_inside");

    /**
     * Epoch milliseconds before which no portal reacts to this player again. Debounces a player
     * stepping out of a portal and straight back in.
     */
    public static final Tag<Long> PORTAL_COOLDOWN = Tag.Transient("portal_cooldown");

    private Tags() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
