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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reloads the lobby's configuration at runtime: loads a fresh snapshot, diffs it against the live
 * configuration, applies the diff and restarts only the modules it affects - with a fallback to a
 * module's previous values if it rejects the new ones.
 *
 * <p>See {@code openspec/changes/config-reload-feature-flags/design.md}, decisions 1 and 3.
 *
 * <p><b>Threading.</b> Loading the fresh snapshot and computing the diff is I/O (file reads) and
 * runs
 * on {@code workerExecutor} - production wires a virtual-thread executor there, so the tick thread
 * is
 * never blocked. Applying the diff and restarting modules must run on the tick thread and is
 * dispatched to {@code tickExecutor} - production wires {@code SchedulerManager#scheduleNextTick}
 * there. Neither executor is ever blocked by this class: the two stages are chained through
 * {@link CompletableFuture}, not through a blocking wait.
 *
 * <p><b>Never runs in parallel.</b> A single run is in flight at a time. A {@link #reload()} call
 * that arrives while one is already running does not start a second one; it is recorded and, once
 * the running one finishes, exactly one further run serves every call that arrived in the meantime
 * -
 * they all complete with that one run's result.
 */
public final class ConfigReloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigReloader.class);

    private final ConfigSnapshotSource source;
    private final LiveConfig liveConfig;
    private final ModuleRestarter restarter;
    private final Executor workerExecutor;
    private final Executor tickExecutor;

    private final Object lock = new Object();
    private boolean running;
    private List<CompletableFuture<ReloadResult>> pendingWaiters = new ArrayList<>();

    public ConfigReloader(
                          ConfigSnapshotSource source, LiveConfig liveConfig, ModuleRestarter restarter, Executor workerExecutor, Executor tickExecutor) {
        this.source = Objects.requireNonNull(source, "source");
        this.liveConfig = Objects.requireNonNull(liveConfig, "liveConfig");
        this.restarter = Objects.requireNonNull(restarter, "restarter");
        this.workerExecutor = Objects.requireNonNull(workerExecutor, "workerExecutor");
        this.tickExecutor = Objects.requireNonNull(tickExecutor, "tickExecutor");
    }

    /**
     * Triggers a reload. Safe to call from any thread (the console command, the file watcher's
     * recurring task, ...).
     *
     * @return a future completed with this call's {@link ReloadResult} - which run actually served
     *         it
     *         depends on whether one was already in flight (see the class javadoc)
     */
    public CompletableFuture<ReloadResult> reload() {
        CompletableFuture<ReloadResult> future = new CompletableFuture<>();
        boolean startNow;
        synchronized (lock) {
            if (running) {
                pendingWaiters.add(future);
                startNow = false;
            } else {
                running = true;
                startNow = true;
            }
        }
        if (startNow) {
            runOnce(List.of(future));
        }
        return future;
    }

    private void runOnce(List<CompletableFuture<ReloadResult>> waiters) {
        CompletableFuture.supplyAsync(this::loadAndDiff, workerExecutor).thenCompose(this::applyOnTickThreadIfNeeded).whenComplete((result, error) -> onRunComplete(result, error, waiters));
    }

    private Stage loadAndDiff() {
        Map<String, String> fresh;
        try {
            fresh = source.load();
        } catch (ConfigSnapshotException exception) {
            LOGGER.warn("Configuration reload rejected: {}", exception.file() + ": " + exception.detail());
            return new Stage.Done(new ReloadResult.Failed(exception.file(), exception.detail()));
        }

        ConfigDiff diff = ConfigDiff.between(liveConfig.currentValues(), fresh);
        if (diff.isEmpty()) {
            LOGGER.debug("Configuration reload found no changes");
            return new Stage.Done(new ReloadResult.Unchanged());
        }
        return new Stage.NeedsApply(diff);
    }

    private CompletableFuture<ReloadResult> applyOnTickThreadIfNeeded(Stage stage) {
        return switch (stage) {
            case Stage.Done(ReloadResult result) -> CompletableFuture.completedFuture(result);
            case Stage.NeedsApply(ConfigDiff diff) ->
                CompletableFuture.supplyAsync(() -> applyDiff(diff), tickExecutor);
        };
    }

    private ReloadResult applyDiff(ConfigDiff diff) {
        liveConfig.apply(diff);

        List<String> restarted = new ArrayList<>();
        List<ReloadResult.RejectedModule> rejected = new ArrayList<>();
        List<String> disabled = new ArrayList<>();

        for (String moduleId : restartOrder(diff)) {
            restartModule(diff, moduleId, restarted, rejected, disabled);
        }

        int changedKeyCount = diff.changedKeys().size() + diff.addedKeys().size() + diff.removedKeys().size();
        LOGGER.info("Configuration reloaded: {} keys changed, modules restarted: {}", changedKeyCount, restarted);

        return new ReloadResult.Applied(restarted, rejected, disabled, diff.featureFlagsChanged());
    }

    /**
     * @param diff the just-applied diff
     * @return {@code diff.affectedModuleIds()}, but in {@code restarter.moduleOrder()}'s
     *         (registration) order instead of that set's own alphabetical iteration order - per
     *         {@code design.md}, decision 3. An affected id {@code restarter.moduleOrder()} does
     *         not
     *         know - its prefix never named a registered module - is left out: it is not a module,
     *         so it is never passed to {@link ModuleRestarter#restart(String)}, which would throw
     *         for an unknown id.
     */
    private List<String> restartOrder(ConfigDiff diff) {
        List<String> order = new ArrayList<>();
        for (String moduleId : restarter.moduleOrder()) {
            if (diff.affectedModuleIds().contains(moduleId)) {
                order.add(moduleId);
            }
        }
        return order;
    }

    private void restartModule(
                               ConfigDiff diff, String moduleId, List<String> restarted, List<ReloadResult.RejectedModule> rejected, List<String> disabled) {
        var keys = diff.keysForModule(moduleId);
        switch (restarter.restart(moduleId)) {
            case ModuleRestartOutcome.Restarted() -> {
                restarted.add(moduleId);
                LOGGER.info("Module {} restarted, changed keys: {}", moduleId, keys);
            }
            case ModuleRestartOutcome.Failed(Throwable cause) -> {
                ConfigDiff.ModuleRevert revert = diff.revertFor(moduleId);
                liveConfig.applyRevert(revert.puts(), revert.removals());
                if (restarter.restart(moduleId) instanceof ModuleRestartOutcome.Restarted) {
                    rejected.add(new ReloadResult.RejectedModule(moduleId, keys, describe(cause)));
                    LOGGER.warn("Module {} rejected new configuration, keeping previous values: {}", moduleId, keys);
                } else {
                    disabled.add(moduleId);
                    LOGGER.error(
                            "Module {} could not be restarted with its previous configuration and is disabled", moduleId);
                }
            }
        }
    }

    private void onRunComplete(ReloadResult result, Throwable error, List<CompletableFuture<ReloadResult>> waiters) {
        List<CompletableFuture<ReloadResult>> next;
        synchronized (lock) {
            next = pendingWaiters;
            pendingWaiters = new ArrayList<>();
            running = !next.isEmpty();
        }

        if (error != null) {
            for (CompletableFuture<ReloadResult> waiter : waiters) {
                waiter.completeExceptionally(error);
            }
        } else {
            for (CompletableFuture<ReloadResult> waiter : waiters) {
                waiter.complete(result);
            }
        }

        if (!next.isEmpty()) {
            runOnce(next);
        }
    }

    private static String describe(Throwable cause) {
        String message = cause.getMessage();
        return message != null ? message : cause.toString();
    }

    /**
     * The outcome of the worker-executor stage ({@link #loadAndDiff()}): either a terminal result
     * already ({@link Done}, for a broken load or an empty diff), or a non-empty diff still needing
     * {@link #applyDiff(ConfigDiff)} on the tick executor ({@link NeedsApply}).
     */
    private sealed interface Stage {

        record Done(ReloadResult result) implements Stage {
        }

        record NeedsApply(ConfigDiff diff) implements Stage {
        }
    }
}
