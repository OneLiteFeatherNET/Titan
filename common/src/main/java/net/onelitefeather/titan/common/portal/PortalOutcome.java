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
package net.onelitefeather.titan.common.portal;

import org.jetbrains.annotations.Contract;

/**
 * What a single movement did at a portal. The listener ignores the value; it exists so the
 * decision is observable - "nothing happened" has four different reasons, and a test that could
 * only look at whether a player was delivered would not be able to tell them apart.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public enum PortalOutcome {

    /** The position is not inside any portal. */
    NO_PORTAL,

    /**
     * The player is inside the same portal they were already inside on the previous movement.
     * This is the re-entry latch: a portal fires on entering, not on standing.
     */
    ALREADY_INSIDE,

    /** The player entered a portal, but too soon after their last portal attempt. */
    COOLING_DOWN,

    /** The feature guarding the destination does not admit this player (US-7.04). */
    DENIED_FEATURE,

    /** No service behind the target could take the player, so nobody was sent (US-7.03). */
    TARGET_UNREACHABLE,

    /** The delivery was attempted and threw. The player stays where they are (US-7.03). */
    DELIVERY_FAILED,

    /** The player was handed to the target server (US-7.01). */
    DELIVERED;

    /**
     * Returns whether this outcome means the player was told something. Used by tests to pin down
     * that a refusal is never silent.
     *
     * @return whether the player receives a message for this outcome
     */
    @Contract(pure = true)
    public boolean isReported() {
        return this == DENIED_FEATURE || this == TARGET_UNREACHABLE || this == DELIVERY_FAILED;
    }
}
