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
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit coverage for {@link NavigatorLayout}: the pure slot -&gt; item computation, including the
 * gray glass pane fill, without a Minestom {@code Inventory}.
 *
 * <p>Every test method takes an (unused) {@link Env} parameter: {@link NavigatorLayout#BLANK} is
 * built with {@code ItemStack.builder(...).customName(...)}, which - unlike the plain
 * {@code ItemStack.of(Material)} other pure-data tests in this codebase use - resolves the stack's
 * {@code Material} through Minestom's registry data once a custom name component is set. That data
 * is only bound once {@link net.minestom.testing.extension.MicrotusExtension} actually creates an
 * {@code Env}, which it only does when a test method asks for one - the same reason other tests
 * that resolve a {@code Material} through Minestom's registry data take one.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorLayoutTest {

    private static NavigatorEntry entry(int slot, Material material, String destination) {
        return new NavigatorEntry(slot, ItemStack.of(material), Component.text(destination), destination);
    }

    @DisplayName("A slot with an entry shows that entry's icon")
    @Test
    void slotWithAnEntryShowsItsIcon(@SuppressWarnings("unused") Env env) {
        NavigatorLayout layout = NavigatorLayout.of(List.of(entry(4, Material.GRASS_BLOCK, "Survival")));

        Assertions.assertEquals(Material.GRASS_BLOCK, layout.itemAt(4).material());
    }

    @DisplayName("A slot without an entry is filled with the blank gray glass pane")
    @Test
    void slotWithoutAnEntryIsBlank(@SuppressWarnings("unused") Env env) {
        NavigatorLayout layout = NavigatorLayout.of(List.of());

        Assertions.assertEquals(NavigatorLayout.BLANK, layout.itemAt(0));
    }

    @DisplayName("entryAt returns the entry occupying a slot")
    @Test
    void entryAtReturnsTheOccupyingEntry(@SuppressWarnings("unused") Env env) {
        NavigatorEntry survival = entry(4, Material.GRASS_BLOCK, "Survival");
        NavigatorLayout layout = NavigatorLayout.of(List.of(survival));

        Assertions.assertEquals(Optional.of(survival), layout.entryAt(4));
    }

    @DisplayName("entryAt returns empty for a blank slot")
    @Test
    void entryAtReturnsEmptyForABlankSlot(@SuppressWarnings("unused") Env env) {
        NavigatorLayout layout = NavigatorLayout.of(List.of());

        Assertions.assertTrue(layout.entryAt(0).isEmpty());
    }

    @DisplayName("The default four entries leave every other slot filled with the same blank glass pane")
    @Test
    void defaultLayoutFillsRemainingSlotsWithGlass(@SuppressWarnings("unused") Env env) {
        NavigatorLayout layout = NavigatorLayout.of(List.of(entry(0, Material.ELYTRA, "ElytraRace"), entry(4, Material.GRASS_BLOCK, "Survival"), entry(5, Material.ENDERMAN_SPAWN_EGG, "cygnus"), entry(8, Material.WOODEN_AXE, "MemberBuild")));

        Assertions.assertEquals(Material.ELYTRA, layout.itemAt(0).material());
        Assertions.assertEquals(Material.GRASS_BLOCK, layout.itemAt(4).material());
        Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, layout.itemAt(5).material());
        Assertions.assertEquals(Material.WOODEN_AXE, layout.itemAt(8).material());
        for (int slot : List.of(1, 2, 3, 6, 7)) {
            Assertions.assertEquals(NavigatorLayout.BLANK, layout.itemAt(slot), "slot " + slot + " should be blank");
        }
    }
}
