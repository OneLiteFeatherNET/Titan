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
package net.onelitefeather.titan.feature.spawn;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ResourcePackPopPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Coverage for {@link ResourcePackClearListener}, wired through {@link SpawnModule} the way it runs
 * in production: a joining player is told to drop every server-pushed resource pack, so a pack a
 * game server left applied does not follow them into the lobby.
 */
@ExtendWith(MicrotusExtension.class)
class ResourcePackClearListenerTest {

    @DisplayName("Configuring a joining player sends one ResourcePackPop without an id (remove all packs)")
    @Test
    void configurationSendsPopWithoutId(Env env) throws InterruptedException {
        Instance instance = env.createFlatInstance();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> new net.minestom.server.coordinate.Pos(0, 64, 0), new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), Mockito.mock(LobbyItems.class));
            module.start();
            try {
                TestConnection connection = env.createConnection();
                Player player = connection.connect(instance);
                Collector<ResourcePackPopPacket> collector = connection.trackIncoming(ResourcePackPopPacket.class);

                // AsyncPlayerConfigurationEvent must be called from a virtual thread.
                Thread.startVirtualThread(() -> env.process().eventHandler().call(new AsyncPlayerConfigurationEvent(player, true))).join();

                ResourcePackPopPacket packet = collector.collect().getFirst();
                Assertions.assertNull(packet.id(), "a pop without id removes every pack, not just one");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Once stopped, configuring a player no longer sends a ResourcePackPop")
    @Test
    void stopLeavesNoListenerBehind(Env env) throws InterruptedException {
        Instance instance = env.createFlatInstance();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SpawnModule module = new SpawnModule(instance, () -> new net.minestom.server.coordinate.Pos(0, 64, 0), new SpawnHeightBoundsFactory().lobbyHeightBounds(), titan.node(), Mockito.mock(LobbyItems.class));
            module.start();
            module.stop();

            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            Collector<ResourcePackPopPacket> collector = connection.trackIncoming(ResourcePackPopPacket.class);

            Thread.startVirtualThread(() -> env.process().eventHandler().call(new AsyncPlayerConfigurationEvent(player, true))).join();

            collector.assertEmpty();
        }
    }
}
