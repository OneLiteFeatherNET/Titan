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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.inject.BeanScope;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.app.feature.protection.ProtectionModule;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.LobbyMap;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Cross-feature coverage for {@link NavigatorModule} and {@link ProtectionModule}, driven through
 * the real {@link BeanScope} since both features' lifecycle methods are package-private. Only one
 * start order is exercised: Aves dispatches inventory clicks before the event-node chain runs, so
 * which feature started first can never matter.
 *
 * <p>The scope is bound to {@code env}'s own instance because the real {@code SpawnModule} would
 * otherwise redirect the joining test player to {@code PlatformBeans}' own, ungenerated
 * {@link InstanceContainer}, hanging {@link Env#createPlayer} forever waiting for chunks that
 * never load. {@link MapProvider#getActiveLobby()} is stubbed to a real {@link LobbyMap} for the
 * same reason - an unstubbed mock would fail {@code SpawnModule}'s listeners instead of
 * teleporting the player.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorProtectionOrderingTest {

    @DisplayName("A navigator click forwards via Deliver and is still cancelled by ProtectionModule, from the real wiring")
    @Test
    @Timeout(30)
    void navigatorClickForwardsAndIsStillCancelled(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        // Registered as both types: a manual .bean(Type, value) registers only that exact type,
        // unlike a generated @Factory, which also registers every supertype it implements.
        BeanScope scope = BeanScope.builder().forTesting().mock(FeatureFlags.class).mock(MapProvider.class, mapProvider -> Mockito.when(mapProvider.getActiveLobby()).thenReturn(new LobbyMap("test", new Pos(0, 65, 0), List.of()))).bean(InstanceContainer.class, (InstanceContainer) instance).bean(Instance.class, instance).bean(Deliver.class, deliver).build();

        try {
            // Neither feature is looked up directly beyond this: both already started themselves,
            // through their own @PostConstruct, while the scope above was being built.
            Assertions.assertNotNull(scope.get(NavigatorModule.class));
            Assertions.assertNotNull(scope.get(ProtectionModule.class));
            LobbyItems lobbyItems = scope.get(LobbyItems.class);

            Player player = env.createPlayer(instance);
            lobbyItems.equip(player);
            ItemStack featherStack = player.getInventory().getItemStack(4);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, featherStack, 0L));
            AbstractInventory openInventory = player.getOpenInventory();
            Assertions.assertNotNull(openInventory, "the navigator must open");

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(4));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled(), "ProtectionModule must still cancel the click");
            Assertions.assertEquals(1, deliver.deliveries().size(), "the navigator click must forward via Deliver");
            Assertions.assertEquals("Survival", deliver.deliveries().get(0).taskName());
        } finally {
            scope.close();
        }
    }

    /** A test-only {@link Deliver} that records every delivery instead of sending one. */
    private static final class RecordingDeliver implements Deliver {

        private record Delivery(Player player, String taskName) {
        }

        private final List<Delivery> deliveries = new ArrayList<>();

        @Override
        public void sendPlayer(Player player, DeliverComponent component) {
            String taskName = component instanceof DeliverComponent.TaskComponent task ? task.taskName() : null;
            this.deliveries.add(new Delivery(player, taskName));
        }

        List<Delivery> deliveries() {
            return List.copyOf(this.deliveries);
        }
    }
}
