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
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DancePermissionsTest {

    private static final UUID PLAYER = new UUID(3, 4);

    @DisplayName("A dance without a permission node needs no check at all")
    @Test
    void freeDanceIsAlwaysAllowed() {
        PermissionService denyEverything = new PermissionService() {
            @Override
            public PermissionResult check(UUID playerId, String permission) {
                throw new AssertionError("a free dance must not ask the permission service");
            }

            @Override
            public String name() {
                return "deny";
            }
        };

        Assertions.assertTrue(new DancePermissions(denyEverything).may(PLAYER, Dance.WAVE), "wave is free and must not depend on any permission");
    }

    @DisplayName("Spin is allowed only when the permission is explicitly granted")
    @Test
    void spinNeedsAnExplicitGrant() {
        Assertions.assertTrue(new DancePermissions(new FakePermissionService().allowEveryone("titan.dance.spin")).may(PLAYER, Dance.SPIN), "a granted spin permission allows spin");
        Assertions.assertFalse(new DancePermissions(new FakePermissionService()).may(PLAYER, Dance.SPIN), "a permission that is not set must not allow spin");
    }

    @DisplayName("An explicit denial of spin blocks it")
    @Test
    void deniedSpinIsBlocked() {
        PermissionService denied = new PermissionService() {
            @Override
            public PermissionResult check(UUID playerId, String permission) {
                return PermissionResult.DENIED;
            }

            @Override
            public String name() {
                return "denied";
            }
        };

        Assertions.assertFalse(new DancePermissions(denied).may(PLAYER, Dance.SPIN), "an explicit denial must block spin");
    }
}
