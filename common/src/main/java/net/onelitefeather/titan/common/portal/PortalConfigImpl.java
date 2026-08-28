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

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The record behind {@link PortalConfig} and the type the JSON file deserialises to.
 *
 * <p>The compact constructor repairs rather than rejects: a file that omits a key, or was written
 * before a key existed, must still yield a usable configuration. Rejecting would take the whole
 * file - including the portals that are fine - out of service over a missing message template.
 *
 * @param portals                 the portal entries as written
 * @param retriggerCooldownMillis cooldown between two portal attempts by the same player
 * @param unreachableMessage      MiniMessage shown when the target cannot take the player
 * @param deniedMessage           MiniMessage shown when the feature does not admit the player
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
record PortalConfigImpl(List<PortalDefinition> portals, long retriggerCooldownMillis,
                        String unreachableMessage,
                        String deniedMessage) implements PortalConfig {

    static final long DEFAULT_COOLDOWN_MILLIS = 3000L;

    static final String DEFAULT_UNREACHABLE_MESSAGE = "<prefix> <red>The server behind this portal is not available right now.";

    static final String DEFAULT_DENIED_MESSAGE = "<prefix> <red>This portal is not open for you.";

    static final PortalConfigImpl DEFAULT = new PortalConfigImpl(List.of(), DEFAULT_COOLDOWN_MILLIS, DEFAULT_UNREACHABLE_MESSAGE, DEFAULT_DENIED_MESSAGE);

    PortalConfigImpl(@Nullable List<PortalDefinition> portals, long retriggerCooldownMillis, @Nullable String unreachableMessage, @Nullable String deniedMessage) {
        // A JSON array may hold a literal null; filtering keeps that from turning into an NPE
        // inside the copy and costs the operator only the one broken entry.
        this.portals = portals == null ? List.of() : portals.stream().filter(Objects::nonNull).toList();
        this.retriggerCooldownMillis = Math.max(0L, retriggerCooldownMillis);
        this.unreachableMessage = unreachableMessage == null || unreachableMessage.isBlank() ? DEFAULT_UNREACHABLE_MESSAGE : unreachableMessage;
        this.deniedMessage = deniedMessage == null || deniedMessage.isBlank() ? DEFAULT_DENIED_MESSAGE : deniedMessage;
    }
}
