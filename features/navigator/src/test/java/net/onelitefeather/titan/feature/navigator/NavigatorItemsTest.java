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
package net.onelitefeather.titan.feature.navigator;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class NavigatorItemsTest {

    @DisplayName("The stack amount is the player count when it is within the stack size")
    @Test
    void amountIsThePlayerCount() {
        Assertions.assertEquals(12, NavigatorItems.amountFor(12), "12 players show a stack of 12");
    }

    @DisplayName("An empty destination still shows a stack of one")
    @Test
    void emptyDestinationShowsOneItem() {
        Assertions.assertEquals(1, NavigatorItems.amountFor(0), "zero players must not produce an empty stack");
    }

    @DisplayName("The stack amount stops at 64, the largest stack size")
    @Test
    void amountIsCappedAt64() {
        Assertions.assertEquals(64, NavigatorItems.amountFor(100), "100 players show a stack of 64");
    }

    @DisplayName("A destination glints only while at least one player is on it")
    @Test
    void glintNeedsAPlayer() {
        Assertions.assertFalse(NavigatorItems.glintsFor(0), "an empty destination does not glint");
        Assertions.assertTrue(NavigatorItems.glintsFor(1), "a destination with one player glints");
    }

    @DisplayName("A live entry with players shows that many items and glints")
    @Test
    void entryStackCarriesTheCountAndTheGlint(Env env) {
        ItemStack stack = Destination.SURVIVAL.item(List.of(Component.empty()), 3);

        Assertions.assertEquals(3, stack.amount(), "the stack amount is the player count");
        Assertions.assertEquals(Boolean.TRUE, stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "the entry glints");
    }

    @DisplayName("A live entry without players shows one item and no glint")
    @Test
    void emptyEntryStackHasNoGlint(Env env) {
        ItemStack stack = Destination.SURVIVAL.item(List.of(Component.empty()), 0);

        Assertions.assertEquals(1, stack.amount(), "an empty entry shows one item");
        Assertions.assertNull(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "an empty entry does not glint");
    }

    @DisplayName("A staging entry keeps its single item and shows no glint")
    @Test
    void stagingEntryKeepsItsStack(Env env) {
        ItemStack stack = Destination.SLENDER_STAGING.item(List.of(Component.empty()));

        Assertions.assertEquals(1, stack.amount(), "the staging submenu does not show the player count");
        Assertions.assertNull(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "the staging submenu does not glint");
    }
}
