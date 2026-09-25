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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link ConfigReloader} against fakes of {@link ConfigSnapshotSource},
 * {@link LiveConfig} and {@link ModuleRestarter}, driven by {@link QueueExecutor} instead of real
 * threads (see {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 3.2).
 */
class ConfigReloaderTest {

    @DisplayName("A broken source applies nothing and reports Failed with file and detail")
    @Test
    void brokenSourceAppliesNothingAndReportsFailed() {
        ScriptedSource source = new ScriptedSource();
        source.willThrow(new ConfigSnapshotException("application.yaml", "line 3, column 5"));
        FakeLiveConfig liveConfig = new FakeLiveConfig(Map.of("sit.offset.y", "0.5"));
        ScriptedModuleRestarter restarter = new ScriptedModuleRestarter();
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor tick = new QueueExecutor();
        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, worker, tick);

        List<ILoggingEvent> events = captureLogs(() -> {
            CompletableFuture<ReloadResult> future = reloader.reload();
            worker.runAll();

            Assertions.assertTrue(future.isDone(), "must complete without needing the tick executor");
            Assertions.assertTrue(tick.isEmpty(), "a broken source must never reach the tick executor");

            ReloadResult result = future.join();
            Assertions.assertInstanceOf(ReloadResult.Failed.class, result, "a broken source must report Failed");
            ReloadResult.Failed failed = (ReloadResult.Failed) result;
            Assertions.assertEquals("application.yaml", failed.file(), "must name the broken file");
            Assertions.assertEquals("line 3, column 5", failed.detail(), "must name the error location");
        });

        Assertions.assertTrue(liveConfig.appliedDiffs.isEmpty(), "nothing must be applied for a broken source");
        Assertions.assertTrue(restarter.restartCalls.isEmpty(), "no module must be restarted for a broken source");
        assertLogged(events, Level.WARN, "Configuration reload rejected: {}");
    }

    @DisplayName("An empty diff reports Unchanged and restarts nothing")
    @Test
    void emptyDiffReportsUnchanged() {
        Map<String, String> state = Map.of("sit.offset.y", "0.5");
        ScriptedSource source = new ScriptedSource();
        source.willReturn(state);
        FakeLiveConfig liveConfig = new FakeLiveConfig(state);
        ScriptedModuleRestarter restarter = new ScriptedModuleRestarter();
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor tick = new QueueExecutor();
        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, worker, tick);

        List<ILoggingEvent> events = captureLogs(() -> {
            CompletableFuture<ReloadResult> future = reloader.reload();
            worker.runAll();

            Assertions.assertTrue(tick.isEmpty(), "an empty diff must never reach the tick executor");
            Assertions.assertInstanceOf(ReloadResult.Unchanged.class, future.join());
        });

        Assertions.assertTrue(liveConfig.appliedDiffs.isEmpty(), "nothing must be applied for an empty diff");
        Assertions.assertTrue(restarter.restartCalls.isEmpty(), "no module must be restarted for an empty diff");
        assertLogged(events, Level.DEBUG, "Configuration reload found no changes");
    }

    @DisplayName("A valid diff is applied once and restarts only the affected modules, in stable order")
    @Test
    void validDiffAppliesOnceAndRestartsAffectedModulesInStableOrder() {
        Map<String, String> old = Map.of(
                "sit.offset.y", "0.5", "elytra.burnDurationTicks", "200", "tickle.cooldownMillis", "4000");
        Map<String, String> updated = Map.of(
                "sit.offset.y", "0.8", "elytra.burnDurationTicks", "400", "tickle.cooldownMillis", "4000");
        ScriptedSource source = new ScriptedSource();
        source.willReturn(updated);
        FakeLiveConfig liveConfig = new FakeLiveConfig(old);
        ScriptedModuleRestarter restarter = new ScriptedModuleRestarter();
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor tick = new QueueExecutor();
        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, worker, tick);

        List<ILoggingEvent> events = captureLogs(() -> {
            CompletableFuture<ReloadResult> future = reloader.reload();
            worker.runAll();
            tick.runAll();

            ReloadResult result = future.join();
            Assertions.assertInstanceOf(ReloadResult.Applied.class, result);
            ReloadResult.Applied applied = (ReloadResult.Applied) result;
            Assertions.assertEquals(
                    List.of("elytra", "sit"), applied.restartedModules(), "must restart only the two affected modules, alphabetically stable");
            Assertions.assertTrue(applied.rejected().isEmpty());
            Assertions.assertTrue(applied.disabledModules().isEmpty());
            Assertions.assertFalse(applied.flagsChanged());
        });

        Assertions.assertEquals(1, liveConfig.appliedDiffs.size(), "the diff must be applied exactly once");
        Assertions.assertEquals(List.of("elytra", "sit"), restarter.restartCalls, "tickle's section did not change, it must not be restarted");
        assertLogged(events, Level.INFO, "Module {} restarted, changed keys: {}");
        assertLogged(events, Level.INFO, "Configuration reloaded: {} keys changed, modules restarted: {}");
    }

    @DisplayName("A failed restart reverts the module's old values, retries and reports it as rejected")
    @Test
    void failedRestartRevertsAndRetriesReportingRejected() {
        Map<String, String> old = Map.of("tickle.cooldownMillis", "4000");
        Map<String, String> updated = Map.of("tickle.cooldownMillis", "-5");
        ScriptedSource source = new ScriptedSource();
        source.willReturn(updated);
        FakeLiveConfig liveConfig = new FakeLiveConfig(old);
        ScriptedModuleRestarter restarter = new ScriptedModuleRestarter();
        RuntimeException cause = new IllegalArgumentException("tickle.cooldownMillis must not be negative");
        restarter.willReturn("tickle", new ModuleRestartOutcome.Failed(cause), new ModuleRestartOutcome.Restarted());
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor tick = new QueueExecutor();
        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, worker, tick);

        List<ILoggingEvent> events = captureLogs(() -> {
            CompletableFuture<ReloadResult> future = reloader.reload();
            worker.runAll();
            tick.runAll();

            ReloadResult result = future.join();
            Assertions.assertInstanceOf(ReloadResult.Applied.class, result);
            ReloadResult.Applied applied = (ReloadResult.Applied) result;
            Assertions.assertTrue(applied.restartedModules().isEmpty(), "a rejected module must not count as restarted");
            Assertions.assertTrue(applied.disabledModules().isEmpty());
            Assertions.assertEquals(1, applied.rejected().size());
            ReloadResult.RejectedModule rejected = applied.rejected().get(0);
            Assertions.assertEquals("tickle", rejected.moduleId());
            Assertions.assertEquals(Set.of("tickle.cooldownMillis"), rejected.keys());
            Assertions.assertEquals("tickle.cooldownMillis must not be negative", rejected.reason());
        });

        Assertions.assertEquals(List.of("tickle", "tickle"), restarter.restartCalls, "must retry exactly once after reverting");
        Assertions.assertEquals(1, liveConfig.revertPuts.size(), "must revert exactly once");
        Assertions.assertEquals(Map.of("tickle.cooldownMillis", "4000"), liveConfig.revertPuts.get(0), "must restore the module's old value");
        Assertions.assertTrue(liveConfig.revertRemovals.get(0).isEmpty());
        assertLogged(events, Level.WARN, "Module {} rejected new configuration, keeping previous values: {}");
    }

    @DisplayName("A module that fails even after the fallback is reported as disabled")
    @Test
    void fallbackFailureReportsModuleAsDisabled() {
        Map<String, String> old = Map.of("tickle.cooldownMillis", "4000");
        Map<String, String> updated = Map.of("tickle.cooldownMillis", "-5");
        ScriptedSource source = new ScriptedSource();
        source.willReturn(updated);
        FakeLiveConfig liveConfig = new FakeLiveConfig(old);
        ScriptedModuleRestarter restarter = new ScriptedModuleRestarter();
        restarter.willReturn(
                "tickle", new ModuleRestartOutcome.Failed(new IllegalStateException("boom")), new ModuleRestartOutcome.Failed(new IllegalStateException("boom again")));
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor tick = new QueueExecutor();
        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, worker, tick);

        List<ILoggingEvent> events = captureLogs(() -> {
            CompletableFuture<ReloadResult> future = reloader.reload();
            worker.runAll();
            tick.runAll();

            ReloadResult.Applied applied = (ReloadResult.Applied) future.join();
            Assertions.assertTrue(applied.restartedModules().isEmpty());
            Assertions.assertTrue(applied.rejected().isEmpty());
            Assertions.assertEquals(List.of("tickle"), applied.disabledModules());
        });

        assertLogged(
                events, Level.ERROR, "Module {} could not be restarted with its previous configuration and is disabled");
    }

    @DisplayName("Requests arriving while a run is in flight coalesce into exactly one further run")
    @Test
    void concurrentRequestsCoalesceIntoOneFurtherRun() {
        Map<String, String> initial = Map.of("sit.offset.y", "0.5");
        Map<String, String> firstUpdate = Map.of("sit.offset.y", "0.8");
        ScriptedSource source = new ScriptedSource();
        source.willReturn(firstUpdate);
        source.willReturn(firstUpdate);
        FakeLiveConfig liveConfig = new FakeLiveConfig(initial);
        ScriptedModuleRestarter restarter = new ScriptedModuleRestarter();
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor tick = new QueueExecutor();
        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, worker, tick);

        CompletableFuture<ReloadResult> first = reloader.reload();
        Assertions.assertEquals(1, worker.size(), "the first request must submit exactly one load");

        CompletableFuture<ReloadResult> second = reloader.reload();
        CompletableFuture<ReloadResult> third = reloader.reload();
        Assertions.assertEquals(1, worker.size(), "requests arriving while a run is in flight must not submit a load of their own");

        worker.runNext();
        tick.runAll();

        Assertions.assertTrue(first.isDone(), "the first request must be served by the first run");
        Assertions.assertFalse(second.isDone(), "the coalesced requests wait for the one further run");
        Assertions.assertFalse(third.isDone());
        Assertions.assertEquals(1, source.callCount(), "only the first run has loaded a snapshot so far");
        Assertions.assertEquals(1, worker.size(), "exactly one further run must have been queued for the coalesced requests");

        worker.runNext();
        tick.runAll();

        Assertions.assertTrue(second.isDone());
        Assertions.assertTrue(third.isDone());
        Assertions.assertEquals(2, source.callCount(), "exactly one further load must have served both coalesced requests");
        Assertions.assertSame(second.join(), third.join(), "both coalesced requests must share the very same result");
        Assertions.assertTrue(worker.isEmpty());
        Assertions.assertTrue(tick.isEmpty());
    }

    /**
     * Captures every line {@link ConfigReloader} logs while {@code action} runs. Temporarily forces
     * the logger's own level to DEBUG - {@code logback.xml} sets root to INFO for production, which
     * would otherwise silently drop the DEBUG line before any appender ever saw it - and restores
     * the
     * original level afterwards so this does not leak into another test (F.I.R.S.T. - Independent).
     */
    private static List<ILoggingEvent> captureLogs(Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(ConfigReloader.class);
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

    /**
     * A {@link ConfigSnapshotSource} fake whose successive {@link #load()} calls are scripted in
     * order.
     */
    private static final class ScriptedSource implements ConfigSnapshotSource {

        private final Deque<Object> responses = new ArrayDeque<>();
        private int callCount;

        void willReturn(Map<String, String> values) {
            responses.addLast(values);
        }

        void willThrow(ConfigSnapshotException exception) {
            responses.addLast(exception);
        }

        @Override
        public Map<String, String> load() {
            callCount++;
            Object response = responses.isEmpty() ? Map.<String, String>of() : responses.removeFirst();
            if (response instanceof ConfigSnapshotException exception) {
                throw exception;
            }
            @SuppressWarnings("unchecked") Map<String, String> values = (Map<String, String>) response;
            return values;
        }

        int callCount() {
            return callCount;
        }
    }

    /**
     * A {@link LiveConfig} fake backed by a plain, mutable map - never the real {@code Config}
     * facade.
     */
    private static final class FakeLiveConfig implements LiveConfig {

        private final Map<String, String> current;
        final List<ConfigDiff> appliedDiffs = new ArrayList<>();
        final List<Map<String, String>> revertPuts = new ArrayList<>();
        final List<Set<String>> revertRemovals = new ArrayList<>();

        FakeLiveConfig(Map<String, String> initial) {
            this.current = new LinkedHashMap<>(initial);
        }

        @Override
        public Map<String, String> currentValues() {
            return Map.copyOf(current);
        }

        @Override
        public void apply(ConfigDiff diff) {
            appliedDiffs.add(diff);
            current.putAll(diff.puts());
            diff.removals().forEach(current::remove);
        }

        @Override
        public void applyRevert(Map<String, String> puts, Set<String> removals) {
            revertPuts.add(puts);
            revertRemovals.add(removals);
            current.putAll(puts);
            removals.forEach(current::remove);
        }
    }

    /** A {@link ModuleRestarter} fake whose outcome per module id can be scripted call by call. */
    private static final class ScriptedModuleRestarter implements ModuleRestarter {

        private final Map<String, Deque<ModuleRestartOutcome>> scripts = new HashMap<>();
        final List<String> restartCalls = new ArrayList<>();

        void willReturn(String moduleId, ModuleRestartOutcome... outcomes) {
            Deque<ModuleRestartOutcome> queue = scripts.computeIfAbsent(moduleId, id -> new ArrayDeque<>());
            queue.addAll(List.of(outcomes));
        }

        @Override
        public ModuleRestartOutcome restart(String moduleId) {
            restartCalls.add(moduleId);
            Deque<ModuleRestartOutcome> script = scripts.get(moduleId);
            if (script != null && !script.isEmpty()) {
                return script.removeFirst();
            }
            return new ModuleRestartOutcome.Restarted();
        }
    }
}
