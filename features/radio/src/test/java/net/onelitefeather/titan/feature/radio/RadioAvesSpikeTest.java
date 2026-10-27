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
package net.onelitefeather.titan.feature.radio;

import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.theevilreaper.aves.i18n.TextData;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.item.TranslatedItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Spike for the per-player radio window: what Aves does with one personal translated builder.
 * Each test pins one observed behaviour, so the design decisions in the PR rest on running code.
 */
@ExtendWith(MicrotusExtension.class)
class RadioAvesSpikeTest {

    private static RadioWindow window(Player player, Material slotZero) {
        RadioWindow window = new RadioWindow(player, Locale.ENGLISH);
        window.setTitleData(new TextData(RadioMessages.MENU_VANILLA));
        window.setLayout(InventoryLayout.fromType(RadioWindow.TYPE));
        window.setDataLayoutFunction(previous -> layout(slotZero));
        return window;
    }

    private static InventoryLayout layout(Material slotZero) {
        InventoryLayout layout = InventoryLayout.fromType(RadioWindow.TYPE);
        layout.setItem(0, ItemStack.of(slotZero));
        return layout;
    }

    private static String titleOf(AbstractInventory inventory) {
        return PlainTextComponentSerializer.plainText().serialize(((Inventory) inventory).getTitle());
    }

    private static Player connect(Env env, Instance instance) {
        return env.createConnection().connect(instance);
    }

    @DisplayName("A personal window shows its contents after one tick")
    @Test
    void openShowsContentAfterATick(Env env) {
        Player player = connect(env, env.createFlatInstance());
        RadioWindow window = window(player, Material.MUSIC_DISC_13);
        window.register();

        window.open(Locale.ENGLISH);
        env.tick();

        AbstractInventory open = player.getOpenInventory();
        Assertions.assertNotNull(open, "the window must be open after open()");
        Assertions.assertEquals(Material.MUSIC_DISC_13, open.getItemStack(0).material(), "the window must show slot 0 after one tick");
        window.unregister();
    }

    @DisplayName("invalidateDataLayout changes an open window in place, on the same inventory")
    @Test
    void invalidateChangesTheOpenWindowInPlace(Env env) {
        Player player = connect(env, env.createFlatInstance());
        RadioWindow window = window(player, Material.MUSIC_DISC_13);
        window.register();
        window.open(Locale.ENGLISH);
        env.tick();
        AbstractInventory before = player.getOpenInventory();

        window.setDataLayoutFunction(previous -> layout(Material.MUSIC_DISC_CAT));
        window.invalidateDataLayout();
        env.tick();

        Assertions.assertSame(before, player.getOpenInventory(), "the same inventory instance must stay open");
        Assertions.assertEquals(Material.MUSIC_DISC_CAT, before.getItemStack(0).material(), "the new data layout must show in the open window");
        window.unregister();
    }

    @DisplayName("unregister closes this window and returns without a recursive close")
    @Test
    void unregisterClosesTheWindow(Env env) {
        Player player = connect(env, env.createFlatInstance());
        RadioWindow window = window(player, Material.MUSIC_DISC_13);
        window.register();
        window.open(Locale.ENGLISH);
        env.tick();

        window.unregister();
        env.tick();

        Assertions.assertNull(player.getOpenInventory(), "unregister must close the window the builder opened");
    }

    @DisplayName("A changed title data does not retitle an open window; the window must be reopened")
    @Test
    void titleChangeNeedsAReopen(Env env) {
        Player player = connect(env, env.createFlatInstance());
        RadioWindow window = window(player, Material.MUSIC_DISC_13);
        window.register();
        window.open(Locale.ENGLISH);
        env.tick();
        AbstractInventory before = player.getOpenInventory();
        String titleBefore = titleOf(before);

        window.setTitleData(new TextData(RadioMessages.MENU_SONGS));
        window.invalidateDataLayout();
        env.tick();

        Assertions.assertEquals(titleBefore, titleOf(player.getOpenInventory()), "Aves keeps the title of a cached translated inventory, so a new title needs a reopen");
        window.unregister();
    }

    @DisplayName("A raw glowing item keeps its glint in the open window")
    @Test
    void glintSurvivesInTheOpenWindow(Env env) {
        Player player = connect(env, env.createFlatInstance());
        RadioWindow window = window(player, Material.MUSIC_DISC_13);
        window.setDataLayoutFunction(previous -> {
            InventoryLayout layout = InventoryLayout.fromType(RadioWindow.TYPE);
            layout.setItem(0, ItemStack.builder(Material.MUSIC_DISC_13).glowing().build());
            return layout;
        });
        window.register();
        window.open(Locale.ENGLISH);
        env.tick();

        Assertions.assertEquals(Boolean.TRUE, player.getOpenInventory().getItemStack(0).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "the glint must reach the open window");
        window.unregister();
    }

    @DisplayName("TranslatedItem.get rebuilds from the material and drops the glint")
    @Test
    void translatedItemDropsTheGlint() {
        ItemStack translated = TranslatedItem.of(ItemStack.builder(Material.MUSIC_DISC_13).glowing()).get(Locale.ENGLISH);

        Assertions.assertNull(translated.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "a translated item must not be used for the glowing title");
    }
}
