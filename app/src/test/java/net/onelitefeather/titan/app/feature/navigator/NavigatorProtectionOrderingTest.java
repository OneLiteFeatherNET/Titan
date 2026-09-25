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

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.feature.protection.ProtectionModule;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Proves {@link NavigatorModule} and {@link ProtectionModule} are independent of each other's
 * enable order (see {@code lobby-modules} spec, "Module sind voneinander unabhängig").
 *
 * <p>{@link ProtectionModule} unconditionally cancels every {@link InventoryPreClickEvent} it sees
 * on its own, module-scoped event node, via the default, cancellation-skipping
 * {@code ModuleContext#listen}. {@link NavigatorModule} registers no
 * {@link InventoryPreClickEvent} listener of its own at all, on any node, so it is never in a race
 * with {@link ProtectionModule} to begin with: its navigator inventory is built by Aves
 * ({@code feature.navigator.NavigatorInventory}), whose click handler is mapped directly onto that
 * inventory rather than hung off a regular {@link net.minestom.server.event.EventNode}, and
 * Minestom dispatches a mapped inventory's handlers before it walks any event node's listener chain
 * - including {@link ProtectionModule}'s. By the time {@link ProtectionModule}'s node could cancel
 * the click, Aves' handler has already cancelled it, forwarded the click through {@code Deliver}
 * and
 * closed the inventory. Both enable orders are exercised here to demonstrate that this ordering
 * never depended on which module started first.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorProtectionOrderingTest {

    @DisplayName("A navigator click forwards via Deliver when NavigatorModule is enabled before ProtectionModule")
    @Test
    void navigatorClickForwardsWhenNavigatorEnabledFirst(Env env) {
        assertNavigatorClickForwards(env, true);
    }

    @DisplayName("A navigator click forwards via Deliver when ProtectionModule is enabled before NavigatorModule")
    @Test
    void navigatorClickForwardsWhenProtectionEnabledFirst(Env env) {
        assertNavigatorClickForwards(env, false);
    }

    private void assertNavigatorClickForwards(Env env, boolean navigatorFirst) {
        RecordingDeliver deliver = new RecordingDeliver();

        try (ModuleHarness harness = ModuleHarness.start(env, (navigator, items) -> {
            NavigatorModule navigatorModule = new NavigatorModule(deliver, navigator, new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true));
            ProtectionModule protectionModule = new ProtectionModule();
            return navigatorFirst ? new LobbyModule[]{navigatorModule, protectionModule} : new LobbyModule[]{protectionModule, navigatorModule};
        })) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            harness.items().equip(player);
            ItemStack feather = player.getInventory().getItemStack(4);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
            AbstractInventory openInventory = player.getOpenInventory();
            Assertions.assertNotNull(openInventory, "the navigator must open regardless of enable order");

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(4));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled(), "ProtectionModule must still cancel the click");
            Assertions.assertEquals(1, deliver.deliveries().size(), "the navigator click must forward via Deliver regardless of which module was enabled first");
            Assertions.assertEquals("Survival", deliver.deliveries().get(0).taskName());
        }
    }
}
