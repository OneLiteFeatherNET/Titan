/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
