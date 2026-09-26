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

import io.avaje.config.ModificationEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reacts to avaje-config's built-in file watcher: registered once with
 * {@code Config.onChange(this)} (see {@link ConfigChangeBootstrap}), it maps a
 * {@link ModificationEvent}'s changed keys to the modules they belong to
 * ({@link ModuleKeys}) and restarts exactly those modules, in registration order, with a
 * fallback to each module's previous values if it rejects the new ones. See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decisions 1 and 2.
 *
 * <p><b>Threading.</b> avaje-config calls {@link #accept(ModificationEvent)} on its own daemon
 * "ConfigTimer" thread. {@link #accept} itself only checks the event's name and, unless it is this
 * handler's own revert, immediately hands the actual work off to {@code tickExecutor} (production:
 * {@code MinecraftServer#getSchedulerManager()}) - never blocking the calling thread. Every other
 * method here runs only on the tick thread, one event at a time (Minestom's own scheduler
 * serializes it), so {@link #snapshot} needs no synchronization of its own.
 *
 * <p><b>Ignoring its own revert.</b> Reverting a module's keys (see {@link #restartModule}) itself
 * publishes a {@code ModificationEvent} named {@code "reload-revert"} - without the check in
 * {@link #accept}, that would trigger another run of this same handler for the very keys it just
 * put back.
 *
 * <p><b>The snapshot.</b> A {@link ModificationEvent} carries no old values (see the design doc's
 * "Faktenprüfung"), so this handler keeps its own flat {@code key -> value} snapshot of the last
 * configuration it accepted - taken once at startup ({@link ConfigChangeBootstrap}) and updated
 * after every handled event: to the new value for every key whose module restarted (or that named
 * no module at all, e.g. a {@code features.*} flag), but left exactly as it was for a module that
 * rejected its new values - so a second, later failure still reverts to the last values that
 * actually ran, not to values already known to be rejected.
 */
public final class ConfigChangeHandler implements Consumer<ModificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChangeHandler.class);

    /**
     * The event name {@link #restartModule} publishes a revert under, via {@link #revertWriter} -
     * see the class javadoc, "Ignoring its own revert".
     */
    private static final String REVERT_EVENT_NAME = "reload-revert";

    private final ModuleRestarter restarter;
    private final ConfigRevertWriter revertWriter;
    private final Executor tickExecutor;

    /** Only ever read or written on the tick thread - see the class javadoc, "Threading". */
    private Map<String, String> snapshot;

    public ConfigChangeHandler(
                               ModuleRestarter restarter, ConfigRevertWriter revertWriter, Executor tickExecutor, Map<String, String> initialSnapshot) {
        this.restarter = Objects.requireNonNull(restarter, "restarter");
        this.revertWriter = Objects.requireNonNull(revertWriter, "revertWriter");
        this.tickExecutor = Objects.requireNonNull(tickExecutor, "tickExecutor");
        this.snapshot = Map.copyOf(Objects.requireNonNull(initialSnapshot, "initialSnapshot"));
    }

    /**
     * @param event the modification avaje-config's file watcher (or any other {@code Config}
     *              writer) published
     */
    @Override
    public void accept(ModificationEvent event) {
        if (REVERT_EVENT_NAME.equals(event.name())) {
            return;
        }
        this.tickExecutor.execute(() -> handle(event));
    }

    /**
     * @return this handler's current flat {@code key -> value} snapshot - the values every
     *         currently running module last restarted with, or, for a rejected module, its
     *         previous values. Exposed for tests; production code never reads it back.
     */
    public Map<String, String> snapshot() {
        return this.snapshot;
    }

    private void handle(ModificationEvent event) {
        Set<String> keys = event.modifiedKeys();
        Set<String> affected = ModuleKeys.affectedModuleIds(keys);
        List<String> toRestart = this.restarter.moduleOrder().stream().filter(affected::contains).toList();

        if (toRestart.isEmpty()) {
            LOGGER.debug("Configuration changed with no affected module: {}", keys);
            updateSnapshot(event, keys, Set.of());
            return;
        }

        Map<String, String> snapshotBeforeEvent = this.snapshot;
        Set<String> revertedKeys = new TreeSet<>();
        for (String moduleId : toRestart) {
            restartModule(moduleId, keys, snapshotBeforeEvent, revertedKeys);
        }
        updateSnapshot(event, keys, revertedKeys);
    }

    private void restartModule(String moduleId, Set<String> allKeys, Map<String, String> snapshotBeforeEvent, Set<String> revertedKeys) {
        Set<String> keysForModule = ModuleKeys.keysForModule(moduleId, allKeys);
        switch (this.restarter.restart(moduleId)) {
            case ModuleRestartOutcome.Restarted() ->
                LOGGER.info("Module {} restarted, changed keys: {}", moduleId, keysForModule);
            case ModuleRestartOutcome.Failed(Throwable cause) -> {
                revert(keysForModule, snapshotBeforeEvent);
                revertedKeys.addAll(keysForModule);
                if (this.restarter.restart(moduleId) instanceof ModuleRestartOutcome.Restarted) {
                    String reason = Causes.rootMessage(cause);
                    LOGGER.warn("Module {} rejected new configuration, keeping previous values: {} ({})", moduleId, keysForModule, reason);
                } else {
                    LOGGER.error("Module {} could not be restarted with its previous configuration and is disabled", moduleId);
                }
            }
        }
    }

    /**
     * @param keysForModule       the failing module's own changed/added/removed keys
     * @param snapshotBeforeEvent the snapshot as it stood before this event - a key present in it
     *                            is put back to its old value, a key absent from it (this event's
     *                            own newly added key) is removed instead
     */
    private void revert(Set<String> keysForModule, Map<String, String> snapshotBeforeEvent) {
        Map<String, String> puts = new LinkedHashMap<>();
        Set<String> removals = new TreeSet<>();
        for (String key : keysForModule) {
            if (snapshotBeforeEvent.containsKey(key)) {
                puts.put(key, snapshotBeforeEvent.get(key));
            } else {
                removals.add(key);
            }
        }
        this.revertWriter.revert(puts, removals);
    }

    /**
     * @param event        the just-handled event, read for its keys' current values
     * @param keys         every key the event touched
     * @param revertedKeys the keys of any module whose revert (see {@link #restartModule}) means
     *                     the snapshot must keep its previous value instead of adopting this
     *                     event's
     */
    private void updateSnapshot(ModificationEvent event, Set<String> keys, Set<String> revertedKeys) {
        Map<String, String> updated = new LinkedHashMap<>(this.snapshot);
        for (String key : keys) {
            if (revertedKeys.contains(key)) {
                continue;
            }
            Optional<String> value = event.configuration().getOptional(key);
            if (value.isPresent()) {
                updated.put(key, value.get());
            } else {
                updated.remove(key);
            }
        }
        this.snapshot = Map.copyOf(updated);
    }
}
