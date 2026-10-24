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

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@code /settings} opens the menu; the German spelling is not a command. */
@ExtendWith(MicrotusExtension.class)
class SettingsCommandIntegrationTest {

    @DisplayName("/settings opens the settings menu for the player who typed it")
    @Test
    void settingsOpensTheMenu(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, new FakeSettingsStore(), SettingsFixtures.registry(SettingsFixtures.TAG), SettingsFixtures.SAME_THREAD);
            module.start();
            try {
                env.process().command().execute(player, SettingsCommand.NAME);
                Assertions.assertNotNull(player.getOpenInventory(), "/settings must open the settings menu");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("/einstellungen is not a command, so no menu opens")
    @Test
    void germanAliasIsUnknown(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, new FakeSettingsStore(), SettingsFixtures.registry(SettingsFixtures.TAG), SettingsFixtures.SAME_THREAD);
            module.start();
            try {
                env.process().command().execute(player, "einstellungen");
                Assertions.assertNull(player.getOpenInventory(), "a German alias must not open the menu, commands are English only");
            } finally {
                module.stop();
            }
        }
    }
}
