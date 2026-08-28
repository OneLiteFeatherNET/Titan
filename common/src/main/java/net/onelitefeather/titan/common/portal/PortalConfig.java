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

import java.util.List;

/**
 * Everything about portals that an operator can change without a code change (US-7.02): the
 * portals themselves, the two refusal messages, and how long a player has to wait between portal
 * attempts.
 *
 * <p>It follows the shape of {@link net.onelitefeather.titan.common.config.AppConfig} - a sealed
 * interface with a package-private record behind it, loaded from JSON by a provider - minus the
 * builder. {@code AppConfig} has one because {@code /app} edits it while the server runs; portals
 * are edited in the file and picked up on load, so a builder would be an unused surface.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public sealed interface PortalConfig permits PortalConfigImpl {

    /** Name of the file portals are read from, next to {@code app.json}. */
    String PORTAL_FILE_NAME = "portals.json";

    /**
     * Returns the configuration used when no {@value #PORTAL_FILE_NAME} exists yet: no portals,
     * and the default messages. Writing this file out is what shows an operator the format.
     *
     * @return the default configuration
     */
    static PortalConfig defaultConfig() {
        return PortalConfigImpl.DEFAULT;
    }

    /**
     * Creates a configuration directly, for tests and for callers that assemble portals in code.
     *
     * @param portals                 the portal entries
     * @param retriggerCooldownMillis the cooldown between two portal attempts by the same player
     * @param unreachableMessage      MiniMessage shown when the target cannot take the player
     * @param deniedMessage           MiniMessage shown when the feature does not admit the player
     * @return a configuration with those values
     */
    static PortalConfig of(List<PortalDefinition> portals, long retriggerCooldownMillis, String unreachableMessage, String deniedMessage) {
        return new PortalConfigImpl(List.copyOf(portals), retriggerCooldownMillis, unreachableMessage, deniedMessage);
    }

    /**
     * The configured portals, still unvalidated - see {@link PortalDefinition#resolve()}.
     *
     * @return the portal entries as written in the file
     */
    List<PortalDefinition> portals();

    /**
     * How long after a portal attempt the same player is ignored by every portal.
     *
     * <p>This is a debounce, not the re-entry guard: standing still in a portal is already handled
     * by the latch in {@link PortalService}. The cooldown covers the player who walks out and
     * straight back in - without it, a portal whose target is down would repeat its message as
     * fast as the player can step across the edge. The window covers the player rather than the
     * portal, which also silences a neighbouring portal for its duration - see
     * {@link net.onelitefeather.titan.common.utils.Tags#PORTAL_COOLDOWN}.
     *
     * <p>A value of {@code 0} - which is also what a file that omits the key deserialises to -
     * turns the debounce off and leaves the latch as the only guard, which is enough to stop a
     * standing player from re-triggering.
     *
     * @return the cooldown in milliseconds
     */
    long retriggerCooldownMillis();

    /**
     * MiniMessage template shown when the target server cannot take the player. Knows the tags
     * {@code <portal>} and {@code <target>}.
     *
     * @return the unreachable-target message
     */
    String unreachableMessage();

    /**
     * MiniMessage template shown when the feature guarding the destination does not admit the
     * player. Knows the tags {@code <portal>} and {@code <target>}.
     *
     * @return the denied message
     */
    String deniedMessage();
}
