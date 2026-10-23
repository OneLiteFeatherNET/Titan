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
package net.onelitefeather.titan.feature.dance;

import java.util.UUID;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;

/** Decides which dances a player may use; a dance without a permission node is open to everyone. */
final class DancePermissions {

    private final PermissionService permissions;

    DancePermissions(PermissionService permissions) {
        this.permissions = permissions;
    }

    boolean may(UUID playerId, Dance dance) {
        return dance.permission().map(node -> this.permissions.check(playerId, node) == PermissionResult.ALLOWED).orElse(true);
    }
}
