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
package net.onelitefeather.titan.feature.npc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;

/** Grants the listed permissions to the listed players; everything else is not set. */
final class FakePermissionService implements PermissionService {

    private final Set<String> granted = new HashSet<>();

    FakePermissionService grant(UUID player, String permission) {
        this.granted.add(player + "/" + permission);
        return this;
    }

    @Override
    public PermissionResult check(UUID playerId, String permission) {
        return this.granted.contains(playerId + "/" + permission) ? PermissionResult.ALLOWED : PermissionResult.NOT_SET;
    }

    @Override
    public String name() {
        return "fake";
    }
}
