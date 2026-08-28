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

import net.minestom.server.entity.Player;

import java.util.UUID;

/**
 * Recognises players who reach the lobby through Geyser.
 *
 * <p>This exists because the reported pack status cannot be trusted for them: for a required
 * pack Geyser answers {@code ACCEPTED}, {@code DOWNLOADED} and {@code SUCCESSFULLY_LOADED} on
 * the player's behalf, even though nothing was ever delivered to the Bedrock client. Since the
 * lie is indistinguishable from a real success, the platform has to be recognised up front
 * instead of inferred from the answer.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
@FunctionalInterface
public interface BedrockDetector {

    /**
     * Whether a player is connected through Geyser.
     *
     * @param playerId the player's unique id
     * @param username the player's name
     * @return {@code true} when the player is a Bedrock player
     */
    boolean isBedrock(UUID playerId, String username);

    /**
     * Whether a player is connected through Geyser.
     *
     * @param player the player to inspect
     * @return {@code true} when the player is a Bedrock player
     */
    default boolean isBedrock(Player player) {
        return this.isBedrock(player.getUuid(), player.getUsername());
    }

    /**
     * A detector that treats every player as a Java player. Useful for tests and for lobbies
     * that are not reachable from Bedrock at all.
     *
     * @return a detector that never reports Bedrock
     */
    static BedrockDetector never() {
        return (playerId, username) -> false;
    }

    /**
     * The detector Titan uses in production.
     *
     * @param namePrefix the Floodgate name prefix, empty to match on the id alone
     * @return a detector recognising Floodgate ids and name prefixes
     */
    static BedrockDetector floodgate(String namePrefix) {
        return new FloodgateBedrockDetector(namePrefix);
    }
}
