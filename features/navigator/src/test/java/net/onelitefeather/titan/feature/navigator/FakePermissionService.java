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
package net.onelitefeather.titan.feature.navigator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;

/**
 * A test-only {@link PermissionService}: every check answers {@link PermissionResult#NOT_SET}
 * until {@link #set(UUID, String, PermissionResult)} says otherwise for that player and
 * permission, or {@link #allowEveryone(String)} grants the permission to all players.
 */
final class FakePermissionService implements PermissionService {

    private final Map<String, PermissionResult> results = new HashMap<>();
    private final Set<String> grantedToEveryone = new HashSet<>();

    FakePermissionService set(UUID playerId, String permission, PermissionResult result) {
        this.results.put(key(playerId, permission), result);
        return this;
    }

    /** Grants {@code permission} to every player who has no explicit result for it. */
    FakePermissionService allowEveryone(String permission) {
        this.grantedToEveryone.add(permission);
        return this;
    }

    @Override
    public PermissionResult check(UUID playerId, String permission) {
        PermissionResult fallback = this.grantedToEveryone.contains(permission) ? PermissionResult.ALLOWED : PermissionResult.NOT_SET;
        return this.results.getOrDefault(key(playerId, permission), fallback);
    }

    @Override
    public String name() {
        return "fake";
    }

    private static String key(UUID playerId, String permission) {
        return playerId + "/" + permission;
    }
}
