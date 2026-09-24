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
package net.onelitefeather.titan.app.module.testing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.app.testutils.EventListenerCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * TDD coverage for {@link ModuleHarness} itself: a module started through it must react to an event
 * fired for a real {@code Env} player, must stop reacting once the harness is closed, and closing
 * one harness must not leave anything behind for the next one started in the same test run. Also
 * covers the accessors ({@link ModuleHarness#items()}, {@link ModuleHarness#navigator()}) and the
 * config-store and standalone (no {@code Env}) entry points.
 */
@ExtendWith(MicrotusExtension.class)
class ModuleHarnessTest {

    /** A player-bound test event, fired directly on the {@code Env}'s global event handler. */
    private record PlayerTestEvent(Player player) implements PlayerEvent {

        @Override
        public Player getPlayer() {
            return this.player;
        }
    }

    /** Counts how many times {@link PlayerTestEvent} reached its listener. */
    private static final class CountingModule implements LobbyModule {

        private final String id;
        private final AtomicInteger counter;

        CountingModule(String id, AtomicInteger counter) {
            this.id = id;
            this.counter = counter;
        }

        @Override
        public String id() {
            return this.id;
        }

        @Override
        public void enable(ModuleContext context) {
            context.listen(PlayerTestEvent.class, event -> this.counter.incrementAndGet());
        }
    }

    private record TestConfig(int value) {
        static final TestConfig DEFAULTS = new TestConfig(3);
    }

    @DisplayName("A module started through the harness receives an event fired for an Env player")
    @Test
    void moduleReceivesAnEventFiredForAnEnvPlayer(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        AtomicInteger calls = new AtomicInteger();

        try (ModuleHarness harness = ModuleHarness.start(env, new CountingModule("counting", calls))) {
            env.process().eventHandler().call(new PlayerTestEvent(player));

            Assertions.assertEquals(1, calls.get(), "the module's listener must run while the harness is open");
        }
    }

    @DisplayName("Once the harness is closed, the module no longer receives events")
    @Test
    void moduleStopsReceivingEventsAfterClose(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        AtomicInteger calls = new AtomicInteger();
        ModuleHarness harness = ModuleHarness.start(env, new CountingModule("counting", calls));
        env.process().eventHandler().call(new PlayerTestEvent(player));
        Assertions.assertEquals(1, calls.get());

        harness.close();

        env.process().eventHandler().call(new PlayerTestEvent(player));
        Assertions.assertEquals(1, calls.get(), "no further event may reach the module once the harness is closed");
    }

    @DisplayName("close() is safe to call more than once")
    @Test
    void closeIsIdempotent(Env env) {
        ModuleHarness harness = ModuleHarness.start(env, new CountingModule("counting", new AtomicInteger()));

        Assertions.assertDoesNotThrow(harness::close);
        Assertions.assertDoesNotThrow(harness::close);
    }

    @DisplayName("Two harnesses started in sequence don't leak listeners into one another")
    @Test
    void twoHarnessesInSequenceDoNotLeakListeners(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        AtomicInteger callsA = new AtomicInteger();
        AtomicInteger callsB = new AtomicInteger();
        int listenersOnGlobalNodeBefore = EventListenerCounter.countListeners(env.process().eventHandler());

        ModuleHarness first = ModuleHarness.start(env, new CountingModule("a", callsA));
        env.process().eventHandler().call(new PlayerTestEvent(player));
        Assertions.assertEquals(1, callsA.get());
        first.close();

        ModuleHarness second = ModuleHarness.start(env, new CountingModule("b", callsB));
        env.process().eventHandler().call(new PlayerTestEvent(player));

        Assertions.assertEquals(1, callsA.get(), "the first harness's listener must not still be attached once it is closed");
        Assertions.assertEquals(1, callsB.get(), "the second harness's own listener must run");
        second.close();
        Assertions.assertEquals(listenersOnGlobalNodeBefore, EventListenerCounter.countListeners(env.process().eventHandler()), "the harness must only ever attach a child node, never a listener directly on the shared global event handler");
    }

    @DisplayName("items() and navigator() expose the same registries the started modules registered against")
    @Test
    void itemsAndNavigatorAccessorsExposeTheSharedRegistries(Env env) {
        LobbyModule module = new LobbyModule() {

            @Override
            public String id() {
                return "wiring";
            }

            @Override
            public void enable(ModuleContext context) {
                context.items().register(new LobbyItem(Key.key("titan:test-item"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(0), (usedBy, event) -> {
                }));
                context.navigator().add(new NavigatorEntry(0, ItemStack.of(Material.COMPASS), Component.text("Test"), "test"));
            }
        };

        try (ModuleHarness harness = ModuleHarness.start(env, module)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);

            harness.items().equip(player);

            Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(0).material(), "harness.items() must be the same registry the module registered its item through");
            Assertions.assertEquals(1, harness.navigator().entries().size(), "harness.navigator() must be the same registry the module added its entry through");
        }
    }

    @DisplayName("A ConfigStore passed to start() is what a started module reads its section from")
    @Test
    void startWithAConfigFileLetsAModuleReadItsOwnSection(Env env, @TempDir Path tempDir) {
        Path configFile = tempDir.resolve("app.json");
        AtomicReference<TestConfig> seen = new AtomicReference<>();
        LobbyModule module = new LobbyModule() {

            @Override
            public String id() {
                return "cfg";
            }

            @Override
            public void enable(ModuleContext context) {
                seen.set(context.config(TestConfig.class, TestConfig.DEFAULTS));
            }
        };

        try (ModuleHarness harness = ModuleHarness.start(env, configFile, module)) {
            Assertions.assertEquals(TestConfig.DEFAULTS, seen.get());
        }

        Assertions.assertTrue(Files.exists(configFile), "enableAll() must flush a freshly created config store");
    }

    @DisplayName("startStandalone() enables and disables a module without booting an Env")
    @Test
    void startStandaloneEnablesAModuleWithoutAnEnv() {
        List<String> log = new ArrayList<>();
        LobbyModule module = new LobbyModule() {

            @Override
            public String id() {
                return "standalone";
            }

            @Override
            public void enable(ModuleContext context) {
                log.add("enabled");
            }

            @Override
            public void disable() {
                log.add("disabled");
            }
        };

        ModuleHarness harness = ModuleHarness.startStandalone(module);
        Assertions.assertEquals(List.of("enabled"), log);

        harness.close();

        Assertions.assertEquals(List.of("enabled", "disabled"), log);
    }
}
