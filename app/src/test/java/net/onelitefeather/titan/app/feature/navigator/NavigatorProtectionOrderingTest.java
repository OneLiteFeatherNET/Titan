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
package net.onelitefeather.titan.app.feature.navigator;

import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.feature.protection.ProtectionModule;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.item.LobbyItems;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Proves {@link NavigatorModule} and {@link ProtectionModule} are independent of each other's start
 * order (see {@code lobby-modules} spec, "Module sind voneinander unabhängig").
 *
 * <p>{@link ProtectionModule} unconditionally cancels every {@link InventoryPreClickEvent} it sees
 * on its own, feature-scoped event node, via the default, cancellation-skipping
 * {@code FeatureNode#on}. {@link NavigatorModule} registers no {@link InventoryPreClickEvent}
 * listener of its own at all, on any node, so it is never in a race with {@link ProtectionModule}
 * to begin with: its shared inventory is built by Aves, whose click handler is mapped directly onto
 * that inventory rather than hung off a regular {@link net.minestom.server.event.EventNode}, and
 * Minestom dispatches a mapped inventory's handlers before it walks any event node's listener
 * chain - including {@link ProtectionModule}'s. By the time {@link ProtectionModule}'s node could
 * cancel the click, Aves' handler has already cancelled it, forwarded the click through
 * {@code Deliver} and closed the inventory. Both start orders are exercised here, with both
 * features built directly on the same parent (titan) node, to demonstrate that this ordering never
 * depended on which one started first.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorProtectionOrderingTest {

    @DisplayName("A navigator click forwards via Deliver when NavigatorModule is started before ProtectionModule")
    @Test
    void navigatorClickForwardsWhenNavigatorStartedFirst(Env env) {
        assertNavigatorClickForwards(env, true);
    }

    @DisplayName("A navigator click forwards via Deliver when ProtectionModule is started before NavigatorModule")
    @Test
    void navigatorClickForwardsWhenProtectionStartedFirst(Env env) {
        assertNavigatorClickForwards(env, false);
    }

    private void assertNavigatorClickForwards(Env env, boolean navigatorFirst) {
        RecordingDeliver deliver = new RecordingDeliver();
        EventNode<Event> titan = EventNode.all("test-titan-" + UUID.randomUUID());
        env.process().eventHandler().addChild(titan);
        NavigatorModule navigatorModule = new NavigatorModule(titan, deliver, new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true));
        ProtectionModule protectionModule = new ProtectionModule(titan);

        if (navigatorFirst) {
            navigatorModule.start();
            protectionModule.start();
        } else {
            protectionModule.start();
            navigatorModule.start();
        }
        LobbyItem feather = new NavigatorItems().navigatorFeather(navigatorModule);
        LobbyItems lobbyItems = new LobbyItems(List.of(feather), titan);

        try {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            lobbyItems.equip(player);
            ItemStack featherStack = player.getInventory().getItemStack(4);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, featherStack, 0L));
            AbstractInventory openInventory = player.getOpenInventory();
            Assertions.assertNotNull(openInventory, "the navigator must open regardless of start order");

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(4));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled(), "ProtectionModule must still cancel the click");
            Assertions.assertEquals(1, deliver.deliveries().size(), "the navigator click must forward via Deliver regardless of which feature started first");
            Assertions.assertEquals("Survival", deliver.deliveries().get(0).taskName());
        } finally {
            navigatorModule.stop();
            protectionModule.stop();
            lobbyItems.stop();
            env.process().eventHandler().removeChild(titan);
        }
    }
}
