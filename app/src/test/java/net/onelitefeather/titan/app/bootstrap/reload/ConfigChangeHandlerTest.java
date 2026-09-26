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
package net.onelitefeather.titan.app.bootstrap.reload;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Configuration;
import io.avaje.config.ModificationEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link ConfigChangeHandler} against fakes of {@link ModuleRestarter} and
 * {@link ConfigRevertWriter}, with a direct ({@link Runnable#run()}) or recording tick executor -
 * never the real {@code Config} facade (no {@code Config} mutator belongs in a unit test, see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 5). The fake
 * {@code ModificationEvent}'s own {@link Configuration} is a standalone, in-memory instance built
 * via {@code Configuration.builder().putAll(...).build()} - not the static facade either. See
 * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 3.3.
 */
class ConfigChangeHandlerTest {

    @DisplayName("Only the modules whose keys changed restart, in registration order")
    @Test
    void onlyAffectedModulesRestartInRegistrationOrder() {
        FakeRestarter restarter = new FakeRestarter(List.of("tickle", "sit", "elytra"));
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of(
                "sit.offset.y", "0.5", "elytra.burnDurationTicks", "200", "tickle.cooldownMillis", "4000"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        ModificationEvent event = event("reload", Set.of("sit.offset.y", "elytra.burnDurationTicks"), Map.of(
                "sit.offset.y", "0.8", "elytra.burnDurationTicks", "400", "tickle.cooldownMillis", "4000"));

        List<ILoggingEvent> events = captureLogs(() -> handler.accept(event));

        Assertions.assertEquals(List.of("sit", "elytra"), restarter.restartCalls, "must restart only the two affected modules, in registration order, not the diff's own alphabetical order");
        assertLogged(events, Level.INFO, "Module {} restarted, changed keys: {}");
    }

    @DisplayName("An id the restarter does not know is never restarted, but its value is still recorded")
    @Test
    void idsNotKnownToTheRestarterAreNotRestarted() {
        FakeRestarter restarter = new FakeRestarter(List.of("sit"));
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("spawn.simulationDistance", "6"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        // "spawn" is deliberately absent from the restarter's known module ids.
        ModificationEvent event = event("reload", Set.of("spawn.simulationDistance"), Map.of("spawn.simulationDistance", "10"));

        handler.accept(event);

        Assertions.assertTrue(restarter.restartCalls.isEmpty(), "restart() must never be called for an id the restarter does not know - it would throw for an unknown module");
        Assertions.assertEquals("10", handler.snapshot().get("spawn.simulationDistance"), "the new value must still be recorded even though nothing was restarted for it");
    }

    @DisplayName("An event named reload-revert is ignored and never even reaches the tick executor")
    @Test
    void revertEventsAreIgnored() {
        FakeRestarter restarter = new FakeRestarter(List.of("sit"));
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        RecordingExecutor tickExecutor = new RecordingExecutor();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("sit.offset.y", "0.5"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, tickExecutor, snapshot);
        ModificationEvent event = event("reload-revert", Set.of("sit.offset.y"), Map.of("sit.offset.y", "0.2"));

        handler.accept(event);

        Assertions.assertTrue(tickExecutor.tasks.isEmpty(), "a reload-revert event must never even be handed to the tick executor");
        Assertions.assertEquals(Map.of("sit.offset.y", "0.5"), handler.snapshot(), "the snapshot must stay untouched for an ignored event");
    }

    @DisplayName("onChange hands off to the tick executor instead of restarting on the calling thread")
    @Test
    void onChangeHandsOffToTheTickExecutorInsteadOfRunningInline() {
        FakeRestarter restarter = new FakeRestarter(List.of("sit"));
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        RecordingExecutor tickExecutor = new RecordingExecutor();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("sit.offset.y", "0.5"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, tickExecutor, snapshot);
        ModificationEvent event = event("reload", Set.of("sit.offset.y"), Map.of("sit.offset.y", "0.8"));

        handler.accept(event);

        Assertions.assertTrue(restarter.restartCalls.isEmpty(), "restart() must not run on the calling thread before the tick executor runs the handed-off task");
        Assertions.assertEquals(1, tickExecutor.tasks.size(), "exactly one task must have been handed to the tick executor");

        tickExecutor.tasks.get(0).run();

        Assertions.assertEquals(List.of("sit"), restarter.restartCalls, "running the handed-off task must perform the restart");
    }

    @DisplayName("A failed restart reverts the module's keys from the snapshot, retries, and reports the innermost cause on WARN")
    @Test
    void failedRestartRevertsFromSnapshotRetriesAndWarns() {
        FakeRestarter restarter = new FakeRestarter(List.of("tickle"));
        RuntimeException cause = new IllegalArgumentException("must not be negative, was -5");
        restarter.willReturn("tickle", new ModuleRestartOutcome.Failed(cause), new ModuleRestartOutcome.Restarted());
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("tickle.cooldownMillis", "4000"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        ModificationEvent event = event("reload", Set.of("tickle.cooldownMillis"), Map.of("tickle.cooldownMillis", "-5"));

        List<ILoggingEvent> events = captureLogs(() -> handler.accept(event));

        Assertions.assertEquals(List.of("tickle", "tickle"), restarter.restartCalls, "must retry exactly once after reverting");
        Assertions.assertEquals(1, revertWriter.puts.size(), "must revert exactly once");
        Assertions.assertEquals(Map.of("tickle.cooldownMillis", "4000"), revertWriter.puts.get(0), "must restore the module's old value from the snapshot");
        Assertions.assertTrue(revertWriter.removals.get(0).isEmpty());
        Assertions.assertEquals(Map.of("tickle.cooldownMillis", "4000"), handler.snapshot(), "the snapshot must keep the rejected module's old value");
        assertLogged(events, Level.WARN, "Module {} rejected new configuration, keeping previous values: {} ({})");
    }

    @DisplayName("The WARN line reports the innermost cause's message, not avaje's wrapper around it")
    @Test
    void rejectionWarnLineReportsTheInnermostCauseNotAvajesWrapper() {
        FakeRestarter restarter = new FakeRestarter(List.of("tickle"));
        // The exact two-level shape avaje-config 5.2's Config.getAs(key, fn) wraps any exception a
        // module's conversion function throws in.
        RuntimeException innerCause = new IllegalArgumentException("must not be negative, was -5");
        RuntimeException avajeWrapper = new IllegalStateException(
                "Failed to convert key: tickle.cooldownMillis with the provided function", innerCause);
        restarter.willReturn("tickle", new ModuleRestartOutcome.Failed(avajeWrapper), new ModuleRestartOutcome.Restarted());
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("tickle.cooldownMillis", "4000"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        ModificationEvent event = event("reload", Set.of("tickle.cooldownMillis"), Map.of("tickle.cooldownMillis", "-5"));

        List<ILoggingEvent> events = captureLogs(() -> handler.accept(event));

        boolean warnHasInnerMessage = events.stream().anyMatch(
                loggingEvent -> loggingEvent.getLevel() == Level.WARN && loggingEvent.getFormattedMessage().contains("must not be negative, was -5") && !loggingEvent.getFormattedMessage().contains("Failed to convert key"));
        Assertions.assertTrue(
                warnHasInnerMessage, "expected the WARN line to contain the inner message and not avaje's wrapper text, got: " + events.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    @DisplayName("A key absent from the snapshot is removed on revert, and stays absent afterwards")
    @Test
    void revertRemovesAKeyThatWasAbsentFromTheSnapshot() {
        FakeRestarter restarter = new FakeRestarter(List.of("tickle"));
        restarter.willReturn("tickle", new ModuleRestartOutcome.Failed(new IllegalStateException("boom")), new ModuleRestartOutcome.Restarted());
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        // tickle.cooldownMillis was never in the snapshot before - a newly added key.
        Map<String, String> snapshot = new LinkedHashMap<>();
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        ModificationEvent event = event("reload", Set.of("tickle.cooldownMillis"), Map.of("tickle.cooldownMillis", "-5"));

        handler.accept(event);

        Assertions.assertTrue(revertWriter.puts.get(0).isEmpty(), "a newly added key has no old value to put back");
        Assertions.assertEquals(Set.of("tickle.cooldownMillis"), revertWriter.removals.get(0), "a key absent from the snapshot must be removed on revert");
        Assertions.assertFalse(handler.snapshot().containsKey("tickle.cooldownMillis"), "the snapshot must still not contain the key after the revert");
    }

    @DisplayName("A second failure after the fallback logs ERROR and leaves the module disabled")
    @Test
    void secondFailureDisablesTheModuleWithError() {
        FakeRestarter restarter = new FakeRestarter(List.of("tickle"));
        restarter.willReturn(
                "tickle", new ModuleRestartOutcome.Failed(new IllegalStateException("boom")), new ModuleRestartOutcome.Failed(new IllegalStateException("boom again")));
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("tickle.cooldownMillis", "4000"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        ModificationEvent event = event("reload", Set.of("tickle.cooldownMillis"), Map.of("tickle.cooldownMillis", "-5"));

        List<ILoggingEvent> events = captureLogs(() -> handler.accept(event));

        assertLogged(events, Level.ERROR, "Module {} could not be restarted with its previous configuration and is disabled");
        Assertions.assertEquals(Map.of("tickle.cooldownMillis", "4000"), handler.snapshot(), "the snapshot must still keep the old value even though the module stayed disabled");
    }

    @DisplayName("A flags-only change restarts no module and is logged on DEBUG")
    @Test
    void flagsOnlyChangeRestartsNoModuleAndLogsDebug() {
        FakeRestarter restarter = new FakeRestarter(List.of("sit", "tickle"));
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("features.NAVIGATOR_SLENDER", "false"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        ModificationEvent event = event("reload", Set.of("features.NAVIGATOR_SLENDER"), Map.of("features.NAVIGATOR_SLENDER", "true"));

        List<ILoggingEvent> events = captureLogs(() -> handler.accept(event));

        Assertions.assertTrue(restarter.restartCalls.isEmpty(), "a features.* change must never restart a module");
        Assertions.assertEquals("true", handler.snapshot().get("features.NAVIGATOR_SLENDER"), "the flag's new value must still be recorded in the snapshot");
        assertLogged(events, Level.DEBUG, "Configuration changed with no affected module: {}");
    }

    @DisplayName("One module rejecting its change does not affect another module restarting successfully in the same event")
    @Test
    void oneModuleFailingDoesNotAffectAnotherSucceedingInTheSameEvent() {
        FakeRestarter restarter = new FakeRestarter(List.of("sit", "tickle"));
        restarter.willReturn(
                "tickle", new ModuleRestartOutcome.Failed(new IllegalArgumentException("must not be negative, was -5")), new ModuleRestartOutcome.Restarted());
        RecordingRevertWriter revertWriter = new RecordingRevertWriter();
        Map<String, String> snapshot = new LinkedHashMap<>(Map.of("sit.offset.y", "0.25", "tickle.cooldownMillis", "4000"));
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, revertWriter, Runnable::run, snapshot);
        ModificationEvent event = event("reload", Set.of("sit.offset.y", "tickle.cooldownMillis"), Map.of("sit.offset.y", "0.5", "tickle.cooldownMillis", "-5"));

        handler.accept(event);

        Assertions.assertEquals(List.of("sit", "tickle", "tickle"), restarter.restartCalls, "sit restarts once, tickle twice (the retry after its revert)");
        Assertions.assertEquals("0.5", handler.snapshot().get("sit.offset.y"), "sit's new value must be kept");
        Assertions.assertEquals("4000", handler.snapshot().get("tickle.cooldownMillis"), "tickle's old value must be kept after its revert");
    }

    /**
     * Builds a fake {@code io.avaje.config.ModificationEvent}: {@code newValues} becomes its own
     * standalone {@link Configuration} (built via {@code Configuration.builder().putAll(...)
     * .build()} - not the static facade), and {@code keys} becomes {@code modifiedKeys()}. A key
     * absent from {@code newValues} models a removed key.
     */
    private static ModificationEvent event(String name, Set<String> keys, Map<String, String> newValues) {
        Configuration configuration = Configuration.builder().putAll(newValues).build();
        return new FakeModificationEvent(name, configuration, keys);
    }

    /**
     * Captures every line {@link ConfigChangeHandler} logs while {@code action} runs, forcing the
     * logger's own level to DEBUG first (production/{@code logback.xml} sets root to INFO, which
     * would otherwise silently drop the DEBUG line), and restores the original level afterwards so
     * this does not leak into another test (F.I.R.S.T. - Independent).
     */
    private static List<ILoggingEvent> captureLogs(Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(ConfigChangeHandler.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            action.run();
        } finally {
            logger.setLevel(originalLevel);
            logger.detachAppender(appender);
        }
        return appender.list;
    }

    private static void assertLogged(List<ILoggingEvent> events, Level level, String message) {
        boolean found = events.stream().anyMatch(event -> event.getLevel() == level && message.equals(event.getMessage()));
        Assertions.assertTrue(
                found, "expected a " + level + " line with template \"" + message + "\", got: " + events.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    private record FakeModificationEvent(String name, Configuration configuration,
                                         Set<String> modifiedKeys) implements ModificationEvent {
    }

    /**
     * A {@link ModuleRestarter} fake whose outcome per module id can be scripted call by call;
     * {@link #moduleOrder()} is fixed at construction, mirroring a restarter that knows a fixed set
     * of modules in registration order.
     */
    private static final class FakeRestarter implements ModuleRestarter {

        private final List<String> order;
        private final Map<String, Deque<ModuleRestartOutcome>> scripts = new HashMap<>();
        final List<String> restartCalls = new ArrayList<>();

        FakeRestarter(List<String> order) {
            this.order = List.copyOf(order);
        }

        void willReturn(String moduleId, ModuleRestartOutcome... outcomes) {
            this.scripts.computeIfAbsent(moduleId, id -> new ArrayDeque<>()).addAll(List.of(outcomes));
        }

        @Override
        public ModuleRestartOutcome restart(String moduleId) {
            this.restartCalls.add(moduleId);
            Deque<ModuleRestartOutcome> script = this.scripts.get(moduleId);
            if (script != null && !script.isEmpty()) {
                return script.removeFirst();
            }
            return new ModuleRestartOutcome.Restarted();
        }

        @Override
        public List<String> moduleOrder() {
            return this.order;
        }
    }

    /** A {@link ConfigRevertWriter} fake that records every call instead of touching the facade. */
    private static final class RecordingRevertWriter implements ConfigRevertWriter {

        final List<Map<String, String>> puts = new ArrayList<>();
        final List<Set<String>> removals = new ArrayList<>();

        @Override
        public void revert(Map<String, String> puts, Set<String> removals) {
            this.puts.add(puts);
            this.removals.add(removals);
        }
    }

    /** An {@link Executor} fake that records every submitted task instead of running it. */
    private static final class RecordingExecutor implements Executor {

        final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            this.tasks.add(command);
        }
    }
}
