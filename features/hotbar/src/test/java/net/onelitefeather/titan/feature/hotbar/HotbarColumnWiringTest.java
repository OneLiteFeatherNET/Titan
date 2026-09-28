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
package net.onelitefeather.titan.feature.hotbar;

import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.GenericType;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.FeatureNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Builds this column's own real Avaje {@link BeanScope} in isolation - only {@code hotbarColumn}
 * is on the classpath here, with the shared {@code titan} {@link EventNode} supplied by hand since
 * no platform module is present to provide it. Proves {@link HotbarLobbyItems} still builds when
 * no other column contributes a {@link net.onelitefeather.titan.core.module.item.LobbyItem}: the
 * generated module lists that dependency as a soft requirement (a {@code List<T>} constructor
 * parameter tolerates being empty), so avaje-inject does not fail the build over it.
 */
@ExtendWith(MicrotusExtension.class)
class HotbarColumnWiringTest {

    @DisplayName("The column's own scope builds a LobbyItems with zero items when nothing else contributes one")
    @Test
    void scopeBuildsLobbyItemsWithZeroItemsWhenNothingContributesOne(Env env) {
        EventNode<Event> titan = EventNode.all("test-hotbar-column-wiring");
        env.process().eventHandler().addChild(titan);

        BeanScope scope = BeanScope.builder().forTesting().bean(FeatureNode.TITAN_NODE, new GenericType<EventNode<Event>>() {
        }.type(), titan).build();

        try {
            HotbarLobbyItems lobbyItems = scope.get(HotbarLobbyItems.class);
            Assertions.assertEquals(0, lobbyItems.itemCount(), "no column contributed a LobbyItem, so the count must be zero, not a build failure");
        } finally {
            scope.close();
        }
    }
}
