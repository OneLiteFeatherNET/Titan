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
import net.onelitefeather.titan.common.permission.TitanPermissionBridge;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link PermissionBridgeConnector}: on start it must publish the active
 * {@link PermissionService} into {@link TitanPermissionBridge}'s static holder, and on stop clear
 * it again, so a CloudNet query never outlives the scope that installed it.
 *
 * <p>{@link TitanPermissionBridge}'s resolver is a static field shared across the classloader
 * boundary to {@code :bridge} (see its own Javadoc), so every test resets it afterwards
 * regardless of execution order.
 */
class PermissionBridgeConnectorTest {

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

    @AfterEach
    void resetTheStaticBridgeResolver() {
        TitanPermissionBridge.setResolver(null);
    }

    @DisplayName("After start, the bridge grants a permission the active service allows")
    @Test
    void afterStartTheBridgeGrantsAnAllowedPermission() {
        PermissionBridgeConnector connector = new PermissionBridgeConnector(new FakePermissionService(PermissionResult.ALLOWED));

        connector.start();

        Assertions.assertTrue(TitanPermissionBridge.hasPermission(UUID.randomUUID(), "titan.command.stop"), "the bridge must grant a permission the active service allows");
    }

    @DisplayName("After start, the bridge denies a permission the active service does not allow")
    @Test
    void afterStartTheBridgeDeniesANotAllowedPermission() {
        PermissionBridgeConnector connector = new PermissionBridgeConnector(new FakePermissionService(PermissionResult.DENIED));

        connector.start();

        Assertions.assertFalse(TitanPermissionBridge.hasPermission(UUID.randomUUID(), "titan.command.stop"), "the bridge must not grant a permission the active service denies");
    }

    @DisplayName("After stop, the bridge denies every permission again")
    @Test
    void afterStopTheBridgeDeniesEveryPermission() {
        PermissionBridgeConnector connector = new PermissionBridgeConnector(new FakePermissionService(PermissionResult.ALLOWED));
        connector.start();

        connector.stop();

        Assertions.assertFalse(TitanPermissionBridge.hasPermission(UUID.randomUUID(), "titan.command.stop"), "no resolver must remain installed once the connector has stopped");
    }
}
