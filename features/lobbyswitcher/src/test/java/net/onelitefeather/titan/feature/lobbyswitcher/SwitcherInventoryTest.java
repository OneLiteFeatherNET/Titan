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

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** What a player sees after clicking the clock. */
@ExtendWith(MicrotusExtension.class)
class SwitcherInventoryTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String name(ItemStack stack) {
        return plain(stack.get(DataComponents.CUSTOM_NAME));
    }

    private static String lore(ItemStack stack) {
        return plain(stack.get(DataComponents.LORE).getFirst());
    }

    private static AbstractInventory opened(SwitcherFixture fixture, Env env) {
        Player player = fixture.join(env.createFlatInstance());
        fixture.use(player);
        env.tick();
        return player.getOpenInventory();
    }

    @DisplayName("Using the clock opens an inventory with the three services in name order")
    @Test
    void listsTheServicesInNameOrder(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            AbstractInventory inventory = opened(fixture, env);

            Assertions.assertNotNull(inventory, "the clock must open an inventory");
            List<String> names = List.of(name(inventory.getItemStack(0)), name(inventory.getItemStack(1)), name(inventory.getItemStack(2)));
            Assertions.assertEquals(List.of("Lobby-1", "Lobby-2", "Lobby-3"), names, "entries by service name");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(3).material(), "slots without an entry hold the filler");
        }
    }

    @DisplayName("The own lobby is marked as current even though the others are joinable")
    @Test
    void marksTheOwnLobby(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            AbstractInventory inventory = opened(fixture, env);

            Assertions.assertEquals(Material.NETHER_STAR, inventory.getItemStack(1).material(), "Lobby-2 is this lobby");
            Assertions.assertTrue(lore(inventory.getItemStack(1)).contains("You are here"), "current lore: " + lore(inventory.getItemStack(1)));
            Assertions.assertNotEquals(Material.NETHER_STAR, inventory.getItemStack(0).material(), "Lobby-1 is not this lobby");
        }
    }

    @DisplayName("Each entry shows online and max of the reading; a full lobby reads as full")
    @Test
    void showsCountsAndStates(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            AbstractInventory inventory = opened(fixture, env);

            Assertions.assertTrue(lore(inventory.getItemStack(0)).contains("3") && lore(inventory.getItemStack(0)).contains("50"), "Lobby-1 counts: " + lore(inventory.getItemStack(0)));
            Assertions.assertTrue(lore(inventory.getItemStack(2)).contains("Full"), "Lobby-3 is full: " + lore(inventory.getItemStack(2)));
        }
    }

    @DisplayName("A German player sees the entries in German")
    @Test
    void rendersPerViewerLocale(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.join(env.createFlatInstance());
            player.setLocale(Locale.GERMAN);

            fixture.use(player);
            env.tick();

            Assertions.assertTrue(lore(player.getOpenInventory().getItemStack(2)).contains("Voll"), "German lore: " + lore(player.getOpenInventory().getItemStack(2)));
        }
    }

    @DisplayName("Opening reads the services once")
    @Test
    void readsOnceAtOpen(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            opened(fixture, env);

            Assertions.assertEquals(1, fixture.counts().reads(), "one read per open, no period yet");
        }
    }
}
