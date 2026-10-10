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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.Optional;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The clock in hotbar slot 8 exists only with the flag on and a known identity. */
@ExtendWith(MicrotusExtension.class)
class LobbySwitcherItemsTest {

    @DisplayName("With the flag and an identity a joining player gets the clock in slot 8")
    @Test
    void flagAndIdentityGiveTheClockInSlotEight(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Instance instance = env.createFlatInstance();

            Player player = fixture.join(instance);

            ItemStack slot = player.getInventory().getItemStack(8);
            Assertions.assertEquals(Material.CLOCK, slot.material(), "slot 8 must hold the clock");
            Assertions.assertEquals(new ItemSlot.Hotbar(8), fixture.item().placement(), "the item must claim hotbar slot 8");
            Assertions.assertEquals("titan:lobbyswitcher", fixture.item().key().asString(), "the item key");
            Assertions.assertEquals("lobbyswitcher", fixture.item().featureId(), "the feature id");
        }
    }

    @DisplayName("Without the flag there is no item and the slot stays empty")
    @Test
    void withoutTheFlagThereIsNoItem(Env env) {
        try (SwitcherFixture fixture = new SwitcherFixture(env, new FakeFeatureFlags(), Optional.of(SwitcherFixture.OWN))) {
            Instance instance = env.createFlatInstance();

            Player player = fixture.join(instance);

            Assertions.assertNull(fixture.item(), "the flag is off, so no item bean");
            Assertions.assertEquals(ItemStack.AIR, player.getInventory().getItemStack(8), "slot 8 must stay empty");
        }
    }

    @DisplayName("Without an identity there is no item and the slot stays empty")
    @Test
    void withoutAnIdentityThereIsNoItem(Env env) {
        try (SwitcherFixture fixture = new SwitcherFixture(env, new FakeFeatureFlags(LobbySwitcherModule.FLAG), Optional.empty())) {
            Instance instance = env.createFlatInstance();

            Player player = fixture.join(instance);

            Assertions.assertNull(fixture.item(), "the lobby does not know itself, so no item bean");
            Assertions.assertEquals(ItemStack.AIR, player.getInventory().getItemStack(8), "slot 8 must stay empty");
        }
    }
}
