/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.core.module;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
@DisplayName("LobbyPlayerBusyEvent")
class LobbyPlayerBusyEventTest {

    @Test
    @DisplayName("names the player it was sent for")
    void namesItsPlayer(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Assertions.assertSame(player, new LobbyPlayerBusyEvent(player, true).getPlayer(), "listeners must be able to find the player whose state changed");
    }

    @Test
    @DisplayName("carries the new busy state as given")
    void carriesBusyState(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Assertions.assertTrue(new LobbyPlayerBusyEvent(player, true).busy(), "a started run or hand-over must be reported as busy");
        Assertions.assertFalse(new LobbyPlayerBusyEvent(player, false).busy(), "a finished run must be reported as not busy");
    }
}
