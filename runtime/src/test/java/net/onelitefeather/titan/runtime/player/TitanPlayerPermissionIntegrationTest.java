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
package net.onelitefeather.titan.runtime.player;

import java.util.UUID;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Cyano {@code Env} coverage for {@link TitanPlayer}: a real, connected player backed by a fake
 * {@link PermissionService} proves the {@link PermissionResult}-to-{@code TriState} mapping
 * {@link TitanPlayer#value(String)} goes through, read back via {@code test(String)}.
 */
@ExtendWith(MicrotusExtension.class)
class TitanPlayerPermissionIntegrationTest {

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

    private static TitanPlayer connect(Env env, Instance instance, PermissionResult result) {
        env.process().connection().setPlayerProvider((connection, gameProfile) -> new TitanPlayer(connection, gameProfile, new FakePermissionService(result)));
        return (TitanPlayer) env.createPlayer(instance);
    }

    @DisplayName("ALLOWED grants the permission")
    @Test
    void allowedGrantsThePermission(Env env) {
        TitanPlayer player = connect(env, env.createFlatInstance(), PermissionResult.ALLOWED);

        Assertions.assertTrue(player.test("titan.command.stop"), "ALLOWED must be granted");
    }

    @DisplayName("DENIED refuses the permission")
    @Test
    void deniedRefusesThePermission(Env env) {
        TitanPlayer player = connect(env, env.createFlatInstance(), PermissionResult.DENIED);

        Assertions.assertFalse(player.test("titan.command.stop"), "DENIED must not be granted");
    }

    @DisplayName("NOT_SET refuses the permission")
    @Test
    void notSetRefusesThePermission(Env env) {
        TitanPlayer player = connect(env, env.createFlatInstance(), PermissionResult.NOT_SET);

        Assertions.assertFalse(player.test("titan.command.stop"), "NOT_SET must not be granted");
    }
}
