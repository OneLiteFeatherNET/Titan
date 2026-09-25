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

import java.util.Map;
import java.util.Set;

/**
 * The current, live configuration state a {@link ConfigReloader} compares a fresh
 * {@link ConfigSnapshotSource} snapshot against, and applies changes to.
 *
 * <p>The production implementation (wired in a later wave) reads {@code Config.asConfiguration()}
 * for {@link #currentValues()} and applies a diff through a single
 * {@code Config.eventBuilder("reload")} (put/remove, then publish) - see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1. A test hands in a
 * fake
 * backed by a plain, mutable {@link Map} instead - never the real facade (no {@code Config}
 * mutator belongs in a unit test, see {@code openspec/changes/avaje-config-facade/design.md},
 * decision 5).
 */
public interface LiveConfig {

    /**
     * @return the live configuration's current flat {@code key -> value} view, compared against a
     *         fresh {@link ConfigSnapshotSource#load()} result via {@link ConfigDiff#between}
     */
    Map<String, String> currentValues();

    /**
     * Applies every put and removal {@code diff} carries ({@link ConfigDiff#puts()},
     * {@link ConfigDiff#removals()}) to the live configuration, as a single atomic step.
     *
     * @param diff the diff to apply in full
     */
    void apply(ConfigDiff diff);

    /**
     * Applies a fallback for exactly one module's keys - the counterpart to
     * {@link #apply(ConfigDiff)}
     * used when a module rejects the values a reload just applied and its previous values must be
     * restored (see {@code ConfigDiff#revertFor(String)}).
     *
     * @param puts     keys to put back with the given (old) value
     * @param removals keys to remove again (the module's keys this reload had newly added)
     */
    void applyRevert(Map<String, String> puts, Set<String> removals);
}
