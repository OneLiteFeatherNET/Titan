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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The join path: a load starts when the player configures, the stored value lands on the tick
 * after it arrived, and a player who left meanwhile gets nothing. The IO executor is driven by
 * hand, so "Otis never answers" is simply a load that never runs.
 */
@ExtendWith(MicrotusExtension.class)
class SettingsJoinIntegrationTest {

    @DisplayName("A join completes while the store never answers")
    @Test
    void joinDoesNotWaitForTheStore(Env env) {
        ManualExecutor io = new ManualExecutor();
        Instance instance = env.createFlatInstance();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, new FakeSettingsStore(), SettingsFixtures.registry(SettingsFixtures.TAG), io);
            module.start();
            try {
                Player player = env.createConnection().connect(instance);
                Assertions.assertTrue(player.isOnline(), "the player must be in the lobby although the load has not run");
                Assertions.assertFalse(module.playerSettings().isEnabled(player.getUuid(), SettingsFixtures.TAG), "until the load arrives the default holds");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A stored value is not applied before the next tick")
    @Test
    void storedValueWaitsForTheTick(Env env) {
        ManualExecutor io = new ManualExecutor();
        Instance instance = env.createFlatInstance();
        FakeSettingsStore store = new FakeSettingsStore();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, store, SettingsFixtures.registry(SettingsFixtures.TAG), io);
            module.start();
            try {
                Player player = env.createConnection().connect(instance);
                store.with(player.getUuid(), SettingKey.parse("titan:tag.enabled"), true);
                io.runAll();
                Assertions.assertFalse(module.playerSettings().isEnabled(player.getUuid(), SettingsFixtures.TAG), "the loaded value must wait for the tick thread");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A stored value is applied on the tick after the load arrived")
    @Test
    void storedValueAppliesOnTheTick(Env env) {
        ManualExecutor io = new ManualExecutor();
        Instance instance = env.createFlatInstance();
        FakeSettingsStore store = new FakeSettingsStore();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, store, SettingsFixtures.registry(SettingsFixtures.TAG), io);
            module.start();
            try {
                Player player = env.createConnection().connect(instance);
                store.with(player.getUuid(), SettingKey.parse("titan:tag.enabled"), true);
                io.runAll();
                env.tick();
                Assertions.assertTrue(module.playerSettings().isEnabled(player.getUuid(), SettingsFixtures.TAG), "the stored true must be applied on the tick");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A player who left during the load gets nothing applied and no notification")
    @Test
    void leavingDuringTheLoadAppliesNothing(Env env) {
        ManualExecutor io = new ManualExecutor();
        Instance instance = env.createFlatInstance();
        FakeSettingsStore store = new FakeSettingsStore();
        List<String> heard = new ArrayList<>();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            SettingsModule module = SettingsFixtures.module(env, titan.node(), SettingsFixtures.ENABLED, store, SettingsFixtures.registry(SettingsFixtures.TAG), io);
            module.start();
            try {
                module.playerSettings().addChangeListener((player, setting, previous, current) -> heard.add(setting.key().toString()));
                Player player = env.createConnection().connect(instance);
                store.with(player.getUuid(), SettingKey.parse("titan:tag.enabled"), true);
                env.process().eventHandler().call(new PlayerDisconnectEvent(player));
                io.runAll();
                env.tick();
                Assertions.assertTrue(heard.isEmpty(), "a load for a player who left must not notify listeners: " + heard);
            } finally {
                module.stop();
            }
        }
    }

    /** An executor that keeps its tasks until the test runs them. */
    private static final class ManualExecutor implements Executor {

        private final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            this.tasks.add(task);
        }

        void runAll() {
            while (!this.tasks.isEmpty()) {
                this.tasks.poll().run();
            }
        }
    }
}
