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
package net.onelitefeather.titan.feature.settings;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The menu as a player sees it: one item per option, lime when on, grey when off, and a click
 * switches it.
 */
@ExtendWith(MicrotusExtension.class)
class SettingsMenuTest {

    @DisplayName("The menu lists one item per registered option, grey when off and lime when on")
    @Test
    void menuShowsOneItemPerOptionWithItsState(Env env) {
        Player player = connect(env);
        PlayerSettingsService service = service();
        SettingsMenu menu = new SettingsMenu(service, SettingsFixtures.registry(SettingsFixtures.TAG, SettingsFixtures.THROW), new SettingsMessages());
        menu.open(player);
        Assertions.assertEquals(Material.GRAY_DYE, player.getOpenInventory().getItemStack(0).material(), "an option that is off must show grey dye");
        Assertions.assertEquals(Material.LIME_DYE, player.getOpenInventory().getItemStack(1).material(), "an option that is on must show lime dye");
    }

    @DisplayName("A click switches the option and shows the menu of the new state")
    @Test
    void clickSwitchesAndShowsTheNewState(Env env) {
        Player player = connect(env);
        PlayerSettingsService service = service();
        SettingsMenu menu = new SettingsMenu(service, SettingsFixtures.registry(SettingsFixtures.TAG), new SettingsMessages());
        menu.open(player);
        menu.toggle(player, SettingsFixtures.TAG);
        Assertions.assertTrue(service.isEnabled(player.getUuid(), SettingsFixtures.TAG), "the click must switch the option on");
        Assertions.assertEquals(Material.LIME_DYE, player.getOpenInventory().getItemStack(0).material(), "the menu after the click must show the new state");
    }

    @DisplayName("A German player reads the option name in German")
    @Test
    void germanPlayerReadsGermanNames(Env env) {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "settings-test"));
        store.register(SettingsMessages.optionKey(SettingsFixtures.TAG.key(), "name"), Locale.GERMAN, "Fangen");
        store.register(SettingsMessages.optionKey(SettingsFixtures.TAG.key(), "name"), Locale.ENGLISH, "Tagging");
        GlobalTranslator.translator().addSource(store);
        try {
            Player player = connect(env);
            player.setLocale(Locale.GERMANY);
            SettingsMenu menu = new SettingsMenu(service(), SettingsFixtures.registry(SettingsFixtures.TAG), new SettingsMessages());
            menu.open(player);
            String name = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().getItemStack(0).get(DataComponents.CUSTOM_NAME));
            Assertions.assertEquals("Fangen", name, "a German player must see the German option name");
        } finally {
            GlobalTranslator.translator().removeSource(store);
        }
    }

    private static Player connect(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        return connection.connect(instance);
    }

    private static PlayerSettingsService service() {
        return new PlayerSettingsService(new WriteBehind(new AdjustableClock(SettingsFixtures.NOW, ZoneOffset.UTC), Duration.ofSeconds(2), 3), new SettingsTelemetry(Telemetry.noop()));
    }
}
