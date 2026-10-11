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
package net.onelitefeather.titan.runtime.bootstrap;

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.ServerListPingType;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.deliver.TitanPlayerLimit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class LobbyPlayerLimitAnnouncerTest {

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void emptyHolder() {
        TitanPlayerLimit.clear();
    }

    @DisplayName("Announcing reports the limit in the ping and hands it to the bridge listener")
    @Test
    void announcesToPingAndBridge(Env env) throws InterruptedException {
        EventNode<Event> node = EventNode.all("test");
        List<Integer> bridge = new ArrayList<>();
        TitanPlayerLimit.onAnnounce(bridge::add);

        new LobbyPlayerLimitAnnouncer(node, new LobbyPlayerLimit(80)).announce();
        ServerListPingEvent ping = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);
        // Minestom asserts that an AsyncEvent is called from a virtual thread.
        Thread.ofVirtual().start(() -> node.call(ping)).join();

        Assertions.assertEquals(80, ping.getStatus().playerInfo().maxPlayers(), "the ping reports the limit");
        Assertions.assertEquals(List.of(80), bridge, "the bridge hears the limit once");
    }
}
