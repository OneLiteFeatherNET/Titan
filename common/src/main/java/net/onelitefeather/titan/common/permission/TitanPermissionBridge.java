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
package net.onelitefeather.titan.common.permission;

import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Cross-classloader bridge for CloudNet permission checks.
 *
 * <p>The CloudNet bridge runs inside a Minestom extension classloader and cannot reach
 * LuckPerms, which lives in the application realm (loaded by its own JarInJar classloader).
 * The two realms only share the application/system classloader that loaded the fat jar, so
 * this holder lives there and exchanges nothing but JDK types: the extension asks the
 * application to resolve a permission for a player UUID without ever referencing a LuckPerms
 * class, and the application installs a resolver backed by LuckPerms without referencing any
 * CloudNet bridge class.
 */
public final class TitanPermissionBridge {

    private static volatile BiPredicate<UUID, String> resolver;

    private TitanPermissionBridge() {
    }

    /**
     * Installs the permission resolver. Called by the application once LuckPerms is up.
     *
     * @param permissionResolver resolves {@code (playerId, permission) -> hasPermission}
     */
    public static void setResolver(BiPredicate<UUID, String> permissionResolver) {
        resolver = permissionResolver;
    }

    /**
     * Resolves whether the given player holds the permission. Returns {@code false} when no
     * resolver has been installed yet (for example during early startup).
     *
     * @param playerId   the player's unique id
     * @param permission the permission node to check
     * @return whether the player holds the permission
     */
    public static boolean hasPermission(UUID playerId, String permission) {
        BiPredicate<UUID, String> current = resolver;
        return current != null && current.test(playerId, permission);
    }
}
