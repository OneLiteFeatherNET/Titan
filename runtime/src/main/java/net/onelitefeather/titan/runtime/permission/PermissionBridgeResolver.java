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
package net.onelitefeather.titan.runtime.permission;

import java.util.UUID;
import java.util.function.BiPredicate;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;

/**
 * Turns a {@link PermissionService} into the {@link BiPredicate} the CloudNet bridge needs: {@code
 * true} exactly when the service reports {@link PermissionResult#ALLOWED}, so a CloudNet
 * permission query agrees with a lobby permission check.
 */
public final class PermissionBridgeResolver {

    private PermissionBridgeResolver() {
    }

    public static BiPredicate<UUID, String> of(PermissionService permissionService) {
        return (playerId, permission) -> permissionService.check(playerId, permission) == PermissionResult.ALLOWED;
    }
}
