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
package net.onelitefeather.titan.app.module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.observability.TitanObservability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;

/**
 * Covers the {@code lobby-modules} spec scenarios for {@link ModuleRegistry}: start order, reverse
 * shutdown order, no events reaching a module during its own shutdown, listeners and commands
 * disappearing after disable, repeating tasks stopping, a failing {@code enable} aborting startup,
 * and one module's failing listener not affecting another module's.
 */
@ExtendWith(MicrotusExtension.class)
class ModuleRegistryTest {

    /** A plain test event, for scenarios that don't need a player. */
    private record TestEvent() implements Event {
    }

    /** A player-bound test event, for scenarios that exercise {@link TitanObservability} guarding. */
    private record PlayerTestEvent(Player player) implements PlayerEvent {

        @Override
        public Player getPlayer() {
            return this.player;
        }
    }

    private static ModuleRegistry.Builder builder(Env env, EventNode<Event> parent) {
        return ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command());
    }

    @DisplayName("Modules start in registration order, before disableAll ever runs")
    @Test
    void modulesStartInRegistrationOrder(Env env) {
        List<String> log = new ArrayList<>();
        EventNode<Event> parent = EventNode.all("test-start-order");
        ModuleRegistry registry = builder(env, parent).modules(new RecordingModule("a", log), new RecordingModule("b", log), new RecordingModule("c", log)).build();

        registry.enableAll();

        Assertions.assertEquals(List.of("enable:a", "enable:b", "enable:c"), log);
    }

    @DisplayName("Modules stop in the reverse of their registration order")
    @Test
    void modulesStopInReverseOrder(Env env) {
        List<String> log = new ArrayList<>();
        EventNode<Event> parent = EventNode.all("test-stop-order");
        ModuleRegistry registry = builder(env, parent).modules(new RecordingModule("a", log), new RecordingModule("b", log), new RecordingModule("c", log)).build();
        registry.enableAll();
        log.clear();

        registry.disableAll();

        Assertions.assertEquals(List.of("disable:c", "disable:b", "disable:a"), log);
    }

    @DisplayName("An event that arrives while a module's own disable() runs does not reach that module")
    @Test
    void noEventsReachAModuleWhileItIsDisabling(Env env) {
        EventNode<Event> parent = EventNode.all("test-no-events-during-disable");
        AtomicInteger listenerCalls = new AtomicInteger();
        List<String> log = new ArrayList<>();
        // This module's own disable() fires the very event it listens for. If the registry detaches
        // the module's node before calling disable(), the listener below must not run for it.
        RecordingModule module = new RecordingModule("sit", log, context -> context.listen(TestEvent.class, event -> listenerCalls.incrementAndGet()), () -> parent.call(new TestEvent()));
        ModuleRegistry registry = builder(env, parent).modules(module).build();
        registry.enableAll();

        registry.disableAll();

        Assertions.assertEquals(0, listenerCalls.get(), "the module's own disable() must not be able to trigger its own (already detached) listener");
    }

    @DisplayName("Listeners registered by a module are gone once the module is disabled")
    @Test
    void listenersAreRemovedAfterDisable(Env env) {
        EventNode<Event> parent = EventNode.all("test-listeners-removed");
        AtomicInteger listenerCalls = new AtomicInteger();
        List<String> log = new ArrayList<>();
        RecordingModule module = new RecordingModule("sit", log, context -> context.listen(TestEvent.class, event -> listenerCalls.incrementAndGet()), () -> {
        });
        ModuleRegistry registry = builder(env, parent).modules(module).build();
        registry.enableAll();
        parent.call(new TestEvent());
        Assertions.assertEquals(1, listenerCalls.get(), "the listener must fire while the module is enabled");

        registry.disableAll();
        parent.call(new TestEvent());

        Assertions.assertEquals(1, listenerCalls.get(), "no further event may reach the module once it is disabled");
    }

    @DisplayName("A repeating task stops running once its module is disabled")
    @Test
    void repeatingTaskStopsAfterDisable(Env env) {
        EventNode<Event> parent = EventNode.all("test-repeating-task-stops");
        AtomicInteger runs = new AtomicInteger();
        List<String> log = new ArrayList<>();
        RecordingModule module = new RecordingModule("ticker", log, context -> context.tasks().schedule(runs::incrementAndGet, TaskSchedule.immediate(), TaskSchedule.tick(1)), () -> {
        });
        ModuleRegistry registry = builder(env, parent).modules(module).build();
        registry.enableAll();
        env.tick();
        env.tick();
        int runsWhileEnabled = runs.get();
        Assertions.assertTrue(runsWhileEnabled > 0, "the task must have run at least once while the module was enabled");

        registry.disableAll();
        int runsAtDisable = runs.get();
        env.tick();
        env.tick();

        Assertions.assertEquals(runsAtDisable, runs.get(), "the task must not run again after the module is disabled");
    }

    @DisplayName("A command registered by a module disappears once the module is disabled")
    @Test
    void commandsDisappearAfterDisable(Env env) {
        EventNode<Event> parent = EventNode.all("test-commands-disappear");
        Command command = new Command("titan-test-command");
        List<String> log = new ArrayList<>();
        RecordingModule module = new RecordingModule("cmd", log, context -> context.commands().register(command), () -> {
        });
        ModuleRegistry registry = builder(env, parent).modules(module).build();
        registry.enableAll();
        Assertions.assertTrue(env.process().command().commandExists("titan-test-command"), "the command must exist while the module is enabled");

        registry.disableAll();

        Assertions.assertFalse(env.process().command().commandExists("titan-test-command"), "the command must be gone once the module is disabled");
    }

    @DisplayName("A module that throws from enable() aborts startup with an exception naming it")
    @Test
    void enableFailureAbortsStartupNamingTheModule(Env env) {
        EventNode<Event> parent = EventNode.all("test-enable-failure");
        List<String> log = new ArrayList<>();
        RecordingModule ok = new RecordingModule("a", log);
        RecordingModule failing = new RecordingModule("b", log, context -> {
            throw new IllegalStateException("boom");
        }, () -> {
        });
        ModuleRegistry registry = builder(env, parent).modules(ok, failing).build();

        ModuleLifecycleException thrown = Assertions.assertThrows(ModuleLifecycleException.class, registry::enableAll);

        Assertions.assertTrue(thrown.getMessage().contains("b"), "the exception must name the failing module");
        Assertions.assertInstanceOf(IllegalStateException.class, thrown.getCause(), "the original failure must be preserved as the cause");
    }

    @DisplayName("An exception in one module's listener is attributed to that module (via the MDC) and does not crash the lobby")
    @Test
    void exceptionInAModulesListenerDoesNotCrashTheLobby(Env env) {
        TitanObservability.installExceptionHandler();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        EventNode<Event> parent = EventNode.all("test-exception-does-not-crash");
        List<String> log = new ArrayList<>();
        List<String> moduleSeenByHealthyListener = new ArrayList<>();
        AtomicInteger healthyListenerCalls = new AtomicInteger();

        // Registered first, so it always runs before the failing module's listener - which is the
        // part of the chain a single throw cannot have already undone.
        RecordingModule healthy = new RecordingModule("tickle", log, context -> context.listen(PlayerTestEvent.class, event -> {
            moduleSeenByHealthyListener.add(MDC.get("module"));
            healthyListenerCalls.incrementAndGet();
        }), () -> {
        });
        RecordingModule failing = new RecordingModule("sit", log, context -> context.listen(PlayerTestEvent.class, event -> {
            throw new IllegalStateException("boom for " + event.getPlayer().getUsername());
        }), () -> {
        });
        ModuleRegistry registry = builder(env, parent).modules(healthy, failing).build();
        registry.enableAll();

        Assertions.assertDoesNotThrow(() -> parent.call(new PlayerTestEvent(player)), "a failing listener must not propagate out of dispatch - the lobby keeps running");
        Assertions.assertEquals(1, healthyListenerCalls.get(), "the healthy module's listener must have run before the failing one");
        Assertions.assertEquals(List.of("tickle"), moduleSeenByHealthyListener, "the healthy listener must see its own module id in the MDC, not the failing one's");

        // The lobby keeps running: a later, unrelated dispatch still reaches every listener as
        // before - the earlier failure left neither module detached nor the dispatch chain broken.
        Assertions.assertDoesNotThrow(() -> parent.call(new PlayerTestEvent(player)));
        Assertions.assertEquals(2, healthyListenerCalls.get(), "the healthy module's listener must keep running on later events");
    }
}
