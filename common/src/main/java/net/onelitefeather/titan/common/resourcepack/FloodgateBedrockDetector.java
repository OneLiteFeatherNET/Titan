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

import java.util.Objects;
import java.util.UUID;

/**
 * Recognises Bedrock players the way Floodgate marks them, without depending on the Floodgate
 * API.
 *
 * <p>Two independent signals, either of which is enough:
 * <ul>
 * <li>Floodgate builds a player's unique id as {@code new UUID(0L, xuid)}, so the whole upper
 * half is zero. A Java id never looks like that: both the online-mode ids from Mojang and the
 * offline-mode ids Minestom derives are version-3 or version-4 UUIDs and carry their version
 * nibble in the upper half.</li>
 * <li>Floodgate prepends a configurable prefix - {@code .} by default - to the Bedrock
 * gamertag. This catches setups where the proxy resolves the linked Java account and the id no
 * longer shows the Floodgate shape.</li>
 * </ul>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class FloodgateBedrockDetector implements BedrockDetector {

    private final String namePrefix;

    /**
     * Creates a detector for a Floodgate setup.
     *
     * @param namePrefix the configured Floodgate name prefix; an empty string disables the name
     *                   check and leaves only the id check
     */
    public FloodgateBedrockDetector(String namePrefix) {
        this.namePrefix = Objects.requireNonNull(namePrefix, "The bedrock name prefix must not be null");
    }

    @Override
    public boolean isBedrock(UUID playerId, String username) {
        if (playerId.getMostSignificantBits() == 0L) {
            return true;
        }
        return !this.namePrefix.isEmpty() && username.startsWith(this.namePrefix);
    }
}
