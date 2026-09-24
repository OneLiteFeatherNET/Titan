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
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit-level coverage for {@link ModuleContext} that does not need a full {@link ModuleRegistry}
 * lifecycle: registering a listener after {@code enable()} has returned, and the order cleanup
 * hooks run in.
 */
@ExtendWith(MicrotusExtension.class)
class ModuleContextTest {

    private record TestEvent() implements Event {
    }

    @DisplayName("moduleId() returns the id the context was created for")
    @Test
    void moduleIdReturnsTheConfiguredId(Env env) {
        ModuleContext context = new ModuleContext("sit", env.process().scheduler(), env.process().command());

        Assertions.assertEquals("sit", context.moduleId());
    }

    @DisplayName("Calling listen() after enable() has returned throws IllegalStateException")
    @Test
    void listenAfterEnableReturnedThrows(Env env) {
        EventNode<Event> parent = EventNode.all("test-context-late-listen");
        AtomicReference<ModuleContext> captured = new AtomicReference<>();
        RecordingModule module = new RecordingModule("late", new ArrayList<>(), captured::set, () -> {
        });
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).modules(module).build();

        registry.enableAll();
        ModuleContext context = captured.get();

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> context.listen(TestEvent.class, event -> {
        }));
        Assertions.assertTrue(thrown.getMessage().contains("late"), "the message must name the offending module");
    }

    @DisplayName("Listening still works while enable() is running")
    @Test
    void listenWorksWhileEnableIsRunning(Env env) {
        EventNode<Event> parent = EventNode.all("test-context-listen-during-enable");
        List<String> log = new ArrayList<>();
        RecordingModule module = new RecordingModule("sit", log, context -> Assertions.assertDoesNotThrow(() -> context.listen(TestEvent.class, event -> {
        })), () -> {
        });
        ModuleRegistry registry = ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).modules(module).build();

        Assertions.assertDoesNotThrow(registry::enableAll);
    }

    @DisplayName("Cleanup hooks run in the reverse order they were added")
    @Test
    void cleanupHooksRunInReverseOrder(Env env) {
        ModuleContext context = new ModuleContext("cleanup", env.process().scheduler(), env.process().command());
        List<Integer> order = new ArrayList<>();
        context.onDisable(() -> order.add(1));
        context.onDisable(() -> order.add(2));
        context.onDisable(() -> order.add(3));

        context.runCleanupHooks();

        Assertions.assertEquals(List.of(3, 2, 1), order);
    }
}
