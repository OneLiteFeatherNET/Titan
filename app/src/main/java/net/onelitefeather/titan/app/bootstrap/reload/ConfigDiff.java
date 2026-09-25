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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The pure comparison between the flat key/value view of the configuration before a reload and the
 * one a fresh {@code Configuration} instance produced, per
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1.
 *
 * <p>A key's <em>module id</em> is the prefix up to (not including) its first {@code .}, e.g.
 * {@code sit} for {@code sit.offset.y}. Two prefixes never name a module: {@code titan} and
 * {@code config} are Titan's/avaje-config's own housekeeping keys, and {@code features} names a
 * feature flag - it only flips {@link #featureFlagsChanged()}, since a flag change never restarts a
 * module (the navigator re-evaluates it on its own the next time it opens).
 *
 * <p>Every accessor and factory method here is pure - no {@code io.avaje.config.Config} call, no
 * I/O - so {@link ConfigReloader} can be exercised against plain {@link Map} fixtures.
 */
public record ConfigDiff(
                         Set<String> changedKeys,
                         Set<String> addedKeys,
                         Set<String> removedKeys,
                         Map<String, String> oldValues,
                         Map<String, String> newValues,
                         Set<String> affectedModuleIds,
                         boolean featureFlagsChanged) {

    private static final Set<String> NO_MODULE_PREFIXES = Set.of("titan", "config");
    private static final String FEATURES_PREFIX = "features";

    /**
     * Defensively copies every collection so a caller mutating the map it built {@code between}
     * from
     * afterwards cannot reach into this diff. {@code affectedModuleIds} is copied into a fresh,
     * unmodifiable {@link TreeSet} rather than through {@link Set#copyOf}: {@code Set.copyOf}'s
     * iteration order is deliberately unspecified, which would silently break the alphabetically
     * stable restart order {@link ConfigReloader} relies on.
     */
    public ConfigDiff {
        changedKeys = Set.copyOf(changedKeys);
        addedKeys = Set.copyOf(addedKeys);
        removedKeys = Set.copyOf(removedKeys);
        oldValues = Map.copyOf(oldValues);
        newValues = Map.copyOf(newValues);
        affectedModuleIds = Collections.unmodifiableSet(new TreeSet<>(affectedModuleIds));
    }

    /**
     * @param old     the configuration's flat values before the reload
     * @param updated the flat values a fresh, fully rebuilt configuration produced
     * @return the diff between the two - empty ({@link #isEmpty()}) when every key is unchanged
     */
    public static ConfigDiff between(Map<String, String> old, Map<String, String> updated) {
        Objects.requireNonNull(old, "old");
        Objects.requireNonNull(updated, "updated");

        Set<String> changed = new TreeSet<>();
        Set<String> added = new TreeSet<>();
        Set<String> removed = new TreeSet<>();
        Map<String, String> oldValues = new LinkedHashMap<>();
        Map<String, String> newValues = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : updated.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();
            if (!old.containsKey(key)) {
                added.add(key);
                newValues.put(key, newValue);
            } else if (!Objects.equals(old.get(key), newValue)) {
                changed.add(key);
                oldValues.put(key, old.get(key));
                newValues.put(key, newValue);
            }
        }
        for (Map.Entry<String, String> entry : old.entrySet()) {
            String key = entry.getKey();
            if (!updated.containsKey(key)) {
                removed.add(key);
                oldValues.put(key, entry.getValue());
            }
        }

        Set<String> affectedModuleIds = new TreeSet<>();
        boolean featureFlagsChanged = false;
        for (String key : allTouchedKeys(changed, added, removed)) {
            String prefix = modulePrefix(key);
            if (FEATURES_PREFIX.equals(prefix)) {
                featureFlagsChanged = true;
            } else if (!NO_MODULE_PREFIXES.contains(prefix)) {
                affectedModuleIds.add(prefix);
            }
        }

        return new ConfigDiff(changed, added, removed, oldValues, newValues, affectedModuleIds, featureFlagsChanged);
    }

    /**
     * @return {@code true} when {@code old} and {@code updated} carried exactly the same keys and
     *         values - nothing changed, was added or was removed
     */
    public boolean isEmpty() {
        return changedKeys.isEmpty() && addedKeys.isEmpty() && removedKeys.isEmpty();
    }

    /**
     * @return every changed or newly added key, mapped to its new value - what a {@link LiveConfig}
     *         applies via {@code Config.eventBuilder(...).put(...)}
     */
    public Map<String, String> puts() {
        Map<String, String> puts = new LinkedHashMap<>();
        for (String key : changedKeys) {
            puts.put(key, newValues.get(key));
        }
        for (String key : addedKeys) {
            puts.put(key, newValues.get(key));
        }
        return Map.copyOf(puts);
    }

    /**
     * @return the keys a {@link LiveConfig} removes via
     *         {@code Config.eventBuilder(...).remove(...)}
     */
    public Set<String> removals() {
        return removedKeys;
    }

    /**
     * @param moduleId a module id, as it appears in {@link #affectedModuleIds()}
     * @return every changed, added or removed key that belongs to {@code moduleId}, for the reload
     *         command's and the log lines' "changed keys" list
     */
    public Set<String> keysForModule(String moduleId) {
        String prefix = moduleId + ".";
        Set<String> keys = new TreeSet<>();
        for (String key : changedKeys) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        for (String key : addedKeys) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        for (String key : removedKeys) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    /**
     * @param moduleId a module id, as it appears in {@link #affectedModuleIds()}
     * @return the values a {@link LiveConfig} must apply to undo exactly this module's part of the
     *         diff - the old value for every changed or removed key of that module (put back), and
     *         every added key of that module (removed again) - so a module that fails to restart
     *         with the new values can be retried with the values it had before this reload
     */
    public ModuleRevert revertFor(String moduleId) {
        String prefix = moduleId + ".";
        Map<String, String> revertPuts = new LinkedHashMap<>();
        for (String key : changedKeys) {
            if (key.startsWith(prefix)) {
                revertPuts.put(key, oldValues.get(key));
            }
        }
        for (String key : removedKeys) {
            if (key.startsWith(prefix)) {
                revertPuts.put(key, oldValues.get(key));
            }
        }
        Set<String> revertRemovals = new TreeSet<>();
        for (String key : addedKeys) {
            if (key.startsWith(prefix)) {
                revertRemovals.add(key);
            }
        }
        return new ModuleRevert(Map.copyOf(revertPuts), Set.copyOf(revertRemovals));
    }

    private static Set<String> allTouchedKeys(Set<String> changed, Set<String> added, Set<String> removed) {
        Set<String> keys = new TreeSet<>(changed);
        keys.addAll(added);
        keys.addAll(removed);
        return keys;
    }

    private static String modulePrefix(String key) {
        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(0, dot);
    }

    /**
     * The values {@link LiveConfig#applyRevert(Map, Set)} needs to restore exactly one module's
     * keys
     * to what they were before a reload - see {@link #revertFor(String)}.
     *
     * @param puts     the keys to put back with their old value
     * @param removals the keys to remove again - the ones this reload had newly added for that
     *                 module
     */
    public record ModuleRevert(Map<String, String> puts, Set<String> removals) {

        public ModuleRevert {
            puts = Map.copyOf(puts);
            removals = Set.copyOf(removals);
        }
    }
}
