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
 * Cross-classloader bridge for CloudNet permission checks. The CloudNet bridge extension cannot
 * reach LuckPerms, which lives in the application's own classloader, so this holder lives on the
 * shared application/system classloader and exchanges only JDK types; until the application
 * installs a resolver, {@link #hasPermission} returns {@code false}.
 */
public final class TitanPermissionBridge {

    private static volatile BiPredicate<UUID, String> resolver;

    private TitanPermissionBridge() {
    }

    public static void setResolver(BiPredicate<UUID, String> permissionResolver) {
        resolver = permissionResolver;
    }

    public static boolean hasPermission(UUID playerId, String permission) {
        BiPredicate<UUID, String> current = resolver;
        return current != null && current.test(playerId, permission);
    }
}
