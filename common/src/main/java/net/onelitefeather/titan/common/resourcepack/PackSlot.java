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
package net.onelitefeather.titan.common.resourcepack;

/**
 * The role a resource pack plays in the stack the lobby delivers.
 *
 * <p>Since 1.20.3 a client can hold several packs at once, each addressed by its own
 * identifier. Titan uses exactly two roles: a large {@link #BASE} pack that stays in the
 * client cache across season changes, and a small {@link #SEASON} delta that is popped and
 * pushed on its own when the season turns.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public enum PackSlot {

    /**
     * The long-lived pack. Its identifier never changes, so the client keeps it cached
     * across season changes and server switches.
     */
    BASE,

    /**
     * The seasonal delta pack. It carries its own identifier and is the only pack removed
     * and replaced when the season changes.
     */
    SEASON
}
