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

/** Stopping the module leaves nothing behind: no join listener, no command. */
@ExtendWith(MicrotusExtension.class)
class SettingsModuleLifecycleTest {

    @DisplayName("After stop a join no longer loads anything from the store")
    @Test
    void stopLeavesNoJoinListenerBehind(Env env) {
        Instance instance = env.createFlatInstance();
        FakeSettingsStore store = new FakeSettingsStore();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, store, SettingsFixtures.registry(SettingsFixtures.TAG), SettingsFixtures.SAME_THREAD);
            module.start();
            module.stop();
            env.createConnection().connect(instance);
            Assertions.assertEquals(0, store.loads(), "a stopped module must not react to joins");
        }
    }

    @DisplayName("After stop /settings does nothing")
    @Test
    void stopRemovesTheCommand(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, new FakeSettingsStore(), SettingsFixtures.registry(SettingsFixtures.TAG), SettingsFixtures.SAME_THREAD);
            module.start();
            module.stop();
            env.process().command().execute(player, SettingsCommand.NAME);
            Assertions.assertNull(player.getOpenInventory(), "a stopped module must not leave /settings registered");
        }
    }

    @DisplayName("A disabled module sends nothing to the store on join")
    @Test
    void disabledModuleNeverLoads(Env env) {
        Instance instance = env.createFlatInstance();
        FakeSettingsStore store = new FakeSettingsStore();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.DISABLED, store, SettingsFixtures.registry(SettingsFixtures.TAG), SettingsFixtures.SAME_THREAD);
            module.start();
            try {
                env.createConnection().connect(instance);
                Assertions.assertEquals(0, store.loads(), "a disabled feature must not request anything from the store");
            } finally {
                module.stop();
            }
        }
    }
}
