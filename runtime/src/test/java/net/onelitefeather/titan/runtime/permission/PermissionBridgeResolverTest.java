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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link PermissionBridgeResolver#of(PermissionService)}: the returned predicate
 * must agree with the lobby's own reading of a {@link PermissionResult}.
 */
class PermissionBridgeResolverTest {

    private record FakePermissionService(PermissionResult result) implements PermissionService {
        @Override
        public PermissionResult check(UUID playerId, String permission) {
            return this.result;
        }

        @Override
        public String name() {
            return "fake";
        }
    }

    @DisplayName("ALLOWED resolves to true")
    @Test
    void allowedResolvesToTrue() {
        BiPredicate<UUID, String> resolver = PermissionBridgeResolver.of(new FakePermissionService(PermissionResult.ALLOWED));

        Assertions.assertTrue(resolver.test(UUID.randomUUID(), "titan.command.stop"));
    }

    @DisplayName("DENIED resolves to false")
    @Test
    void deniedResolvesToFalse() {
        BiPredicate<UUID, String> resolver = PermissionBridgeResolver.of(new FakePermissionService(PermissionResult.DENIED));

        Assertions.assertFalse(resolver.test(UUID.randomUUID(), "titan.command.stop"));
    }

    @DisplayName("NOT_SET resolves to false")
    @Test
    void notSetResolvesToFalse() {
        BiPredicate<UUID, String> resolver = PermissionBridgeResolver.of(new FakePermissionService(PermissionResult.NOT_SET));

        Assertions.assertFalse(resolver.test(UUID.randomUUID(), "titan.command.stop"));
    }
}
