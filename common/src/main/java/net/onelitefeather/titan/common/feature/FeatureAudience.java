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
package net.onelitefeather.titan.common.feature;

import java.util.UUID;

/**
 * Answers the two questions a release stage needs about a player: does the player hold a
 * permission, and is the player a member of a group.
 *
 * <p>This is deliberately not a second permission system. It is the seam that keeps
 * {@code :common} free of LuckPerms types: the production implementation lives in the
 * application module and delegates every answer to LuckPerms, while tests supply a fixture. Only
 * JDK types cross this interface, which is the same rule
 * {@code net.onelitefeather.titan.common.permission.TitanPermissionBridge} follows for the
 * CloudNet bridge.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface FeatureAudience {

    /**
     * Returns an audience that answers {@code false} to everything. Used as the safe default
     * before a real permission backend is available.
     *
     * @return an audience that grants nothing
     */
    static FeatureAudience denyAll() {
        return new FeatureAudience() {

            @Override
            public boolean hasPermission(UUID playerId, String permission) {
                return false;
            }

            @Override
            public boolean inGroup(UUID playerId, String group) {
                return false;
            }
        };
    }

    /**
     * Checks whether the player holds the given permission node.
     *
     * @param playerId   the player's unique id
     * @param permission the permission node, for example {@code titan.feature.internal}
     * @return whether the player holds the permission
     */
    boolean hasPermission(UUID playerId, String permission);

    /**
     * Checks whether the player is a member of the given group, inherited groups included.
     *
     * @param playerId the player's unique id
     * @param group    the group name, for example {@code lite}
     * @return whether the player belongs to the group
     */
    boolean inGroup(UUID playerId, String group);
}
