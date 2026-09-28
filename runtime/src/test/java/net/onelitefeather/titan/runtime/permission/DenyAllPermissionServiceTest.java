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
import net.onelitefeather.titan.core.permission.PermissionResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link DenyAllPermissionService}, the fallback used when no platform module
 * provides a {@link net.onelitefeather.titan.core.permission.PermissionService}.
 */
class DenyAllPermissionServiceTest {

    private final DenyAllPermissionService service = new DenyAllPermissionService();

    @DisplayName("Any permission for any player is not set")
    @Test
    void anyPermissionForAnyPlayerIsNotSet() {
        PermissionResult result = this.service.check(UUID.randomUUID(), "titan.command.stop");

        Assertions.assertEquals(PermissionResult.NOT_SET, result, "the fallback must never grant or deny a permission");
    }

    @DisplayName("A different player and permission still resolve to not set")
    @Test
    void aDifferentPlayerAndPermissionAlsoResolveToNotSet() {
        PermissionResult result = this.service.check(UUID.randomUUID(), "titan.command.other");

        Assertions.assertEquals(PermissionResult.NOT_SET, result, "the fallback must never grant or deny a permission");
    }

    @DisplayName("The service name is deny-all")
    @Test
    void theServiceNameIsDenyAll() {
        Assertions.assertEquals("deny-all", this.service.name(), "the start log identifies this service as deny-all");
    }
}
