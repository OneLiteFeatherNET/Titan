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
import java.util.concurrent.atomic.AtomicReference;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit-level coverage for {@link ModuleContext} that does not need a full {@link ModuleRegistry}
 * lifecycle: registering a listener after {@code enable()} has returned, and the order cleanup
 * hooks run in.
 *
 * <p>Plain unit test: none of this needs a {@link net.minestom.server.entity.Player} or
 * {@link net.minestom.server.instance.Instance}, so it builds {@link ModulePlatformFixture} and
 * {@link ModuleRegistry} from a standalone {@link Scheduler#newScheduler()} and
 * {@link CommandManager} instead of booting a Microtus {@code Env}.
 */
class ModuleContextTest {

    private record TestEvent() implements Event {
    }

    @DisplayName("moduleId() returns the id the context was created for")
    @Test
    void moduleIdReturnsTheConfiguredId() {
        ModuleContext context = new ModuleContext("sit", ModulePlatformFixture.create(Scheduler.newScheduler(), new CommandManager()));

        Assertions.assertEquals("sit", context.moduleId());
    }

    @DisplayName("Calling listen() after enable() has returned throws IllegalStateException")
    @Test
    void listenAfterEnableReturnedThrows() {
        EventNode<Event> parent = EventNode.all("test-context-late-listen");
        AtomicReference<ModuleContext> captured = new AtomicReference<>();
        RecordingModule module = new RecordingModule("late", new ArrayList<>(), captured::set, () -> {
        });
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(Scheduler.newScheduler()).commandManager(new CommandManager()).modules(module).build();

        registry.enableAll();
        ModuleContext context = captured.get();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> context.listen(TestEvent.class, event -> {
        }));
        Assertions.assertTrue(thrown.getMessage().contains("late"), "the message must name the offending module");
    }

    @DisplayName("Listening still works while enable() is running")
    @Test
    void listenWorksWhileEnableIsRunning() {
        EventNode<Event> parent = EventNode.all("test-context-listen-during-enable");
        List<String> log = new ArrayList<>();
        RecordingModule module = new RecordingModule("sit", log, context -> Assertions.assertDoesNotThrow(() -> context.listen(TestEvent.class, event -> {
        })), () -> {
        });
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(Scheduler.newScheduler()).commandManager(new CommandManager()).modules(module).build();

        Assertions.assertDoesNotThrow(registry::enableAll);
    }

    @DisplayName("Cleanup hooks run in the reverse order they were added")
    @Test
    void cleanupHooksRunInReverseOrder() {
        ModuleContext context = new ModuleContext("cleanup", ModulePlatformFixture.create(Scheduler.newScheduler(), new CommandManager()));
        List<Integer> order = new ArrayList<>();
        context.onDisable(() -> order.add(1));
        context.onDisable(() -> order.add(2));
        context.onDisable(() -> order.add(3));

        context.runCleanupHooks();

        Assertions.assertEquals(List.of(3, 2, 1), order);
    }
}
