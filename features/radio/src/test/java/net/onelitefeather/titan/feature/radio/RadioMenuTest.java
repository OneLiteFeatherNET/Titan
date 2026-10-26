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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(MicrotusExtension.class)
class RadioMenuTest {

    @TempDir
    Path radio;

    private static String name(ItemStack stack) {
        return PlainTextComponentSerializer.plainText().serialize(stack.get(DataComponents.CUSTOM_NAME));
    }

    @DisplayName("The mode button opens the next mode of the same menu and cycles back to once")
    @Test
    void modeButtonCyclesTheModeOfTheSameMenu(Env env) {
        Instance instance = env.createFlatInstance();
        try (RadioModuleIntegrationTest.Round round = new RadioModuleIntegrationTest.Round(env, this.radio.resolve("missing"))) {
            Player player = round.connect(env, instance).player();
            round.module.open(player);

            RadioModuleIntegrationTest.click(env, player, RadioMenuLayout.MODE_SLOT);
            Assertions.assertSame(round.module.inventory(RadioMenu.VANILLA, PlayMode.LOOP, player.getLocale()), player.getOpenInventory(), "the mode button must open the vanilla menu in loop mode");
            RadioModuleIntegrationTest.click(env, player, RadioMenuLayout.MODE_SLOT);
            Assertions.assertSame(round.module.inventory(RadioMenu.VANILLA, PlayMode.SHUFFLE, player.getLocale()), player.getOpenInventory(), "the next click must open shuffle");
            RadioModuleIntegrationTest.click(env, player, RadioMenuLayout.MODE_SLOT);
            Assertions.assertSame(round.module.inventory(RadioMenu.VANILLA, PlayMode.ONCE, player.getLocale()), player.getOpenInventory(), "the third click must cycle back to once");
        }
    }

    @DisplayName("The switch opens the other menu in the mode the player has")
    @Test
    void switchOpensTheOtherMenuInTheCurrentMode(Env env) throws Exception {
        TestSongFiles.writeSunrise(this.radio, "sunrise.mid");
        Instance instance = env.createFlatInstance();
        try (RadioModuleIntegrationTest.Round round = new RadioModuleIntegrationTest.Round(env, this.radio)) {
            Player player = round.connect(env, instance).player();
            round.module.open(player);
            RadioModuleIntegrationTest.click(env, player, RadioMenuLayout.MODE_SLOT);
            RadioModuleIntegrationTest.click(env, player, RadioMenuLayout.MODE_SLOT);

            RadioModuleIntegrationTest.click(env, player, RadioMenuLayout.SWITCH_SLOT);

            Assertions.assertSame(round.module.inventory(RadioMenu.SONGS, PlayMode.SHUFFLE, player.getLocale()), player.getOpenInventory(), "the switch must open the songs menu in shuffle mode");
            RadioModuleIntegrationTest.click(env, player, RadioMenuLayout.SWITCH_SLOT);
            Assertions.assertSame(round.module.inventory(RadioMenu.VANILLA, PlayMode.SHUFFLE, player.getLocale()), player.getOpenInventory(), "the switch in the songs menu must go back to the vanilla discs");
        }
    }

    @DisplayName("Six inventories exist after start: two menus in three modes")
    @Test
    void sixInventoriesExistAfterStart(Env env) {
        try (RadioModuleIntegrationTest.Round round = new RadioModuleIntegrationTest.Round(env, this.radio.resolve("missing"))) {
            Set<Inventory> inventories = new HashSet<>();
            for (RadioMenu menu : RadioMenu.values()) {
                for (PlayMode mode : PlayMode.values()) {
                    inventories.add(round.module.inventory(menu, mode, Locale.ENGLISH));
                }
            }

            Assertions.assertEquals(6, inventories.size(), "each menu and mode must have its own inventory");
        }
    }

    @DisplayName("One shared inventory shows each viewer the texts of their own language")
    @Test
    void oneInventoryShowsEachViewerTheirLanguage(Env env) {
        try (RadioModuleIntegrationTest.Round round = new RadioModuleIntegrationTest.Round(env, this.radio.resolve("missing"))) {
            // Aves fills the contents of a new inventory on the next tick.
            RadioModuleIntegrationTest.tick(env, 1);
            String german = name(round.module.inventory(RadioMenu.VANILLA, PlayMode.ONCE, Locale.GERMAN).getItemStack(RadioMenuLayout.STOP_SLOT));
            String english = name(round.module.inventory(RadioMenu.VANILLA, PlayMode.ONCE, Locale.ENGLISH).getItemStack(RadioMenuLayout.STOP_SLOT));

            Assertions.assertEquals("Stopp", german, "the stop button must read Stopp for a German viewer");
            Assertions.assertEquals("Stop", english, "the stop button must read Stop for an English viewer");
        }
    }
}
