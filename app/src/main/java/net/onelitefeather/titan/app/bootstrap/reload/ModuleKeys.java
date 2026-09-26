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

import java.util.Set;
import java.util.TreeSet;

/**
 * The pure key-to-module-id mapping {@link ConfigChangeHandler} uses to decide which modules a
 * {@code io.avaje.config.ModificationEvent}'s changed keys affect - see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1.
 *
 * <p>A key's <em>module id</em> is the prefix up to (not including) its first {@code .}, e.g.
 * {@code sit} for {@code sit.offset.y}. Three prefixes never name a module: {@code titan} and
 * {@code config} are Titan's/avaje-config's own housekeeping keys, and {@code features} names a
 * feature flag - a flag change never restarts a module ({@link ConfigChangeHandler} logs it on
 * DEBUG and moves on; the navigator re-evaluates it on its own the next time it opens).
 *
 * <p>Every method here is pure - no {@code io.avaje.config.Config} call, no I/O - so
 * {@link ConfigChangeHandler} can be exercised against plain {@link Set} fixtures.
 */
final class ModuleKeys {

    private static final Set<String> NO_MODULE_PREFIXES = Set.of("features", "titan", "config");

    private ModuleKeys() {
    }

    /**
     * @param key a configuration key, e.g. {@code sit.offset.y}
     * @return the module id {@code key} belongs to, or {@code null} if {@code key}'s prefix is
     *         {@code features}, {@code titan} or {@code config} - not a module at all
     */
    static String moduleIdOf(String key) {
        String prefix = prefixOf(key);
        return NO_MODULE_PREFIXES.contains(prefix) ? null : prefix;
    }

    /**
     * @param keys the changed, added or removed keys of one
     *             {@code io.avaje.config.ModificationEvent}
     * @return every module id at least one of {@code keys} belongs to, per {@link #moduleIdOf} -
     *         never {@code features}/{@code titan}/{@code config}. The caller (
     *         {@link ConfigChangeHandler}) restarts the ids this method returns that its own
     *         {@link ModuleRestarter#moduleOrder()} actually knows, in <em>that</em> registration
     *         order - not this method's own (alphabetical, for a stable/deterministic result)
     *         order.
     */
    static Set<String> affectedModuleIds(Set<String> keys) {
        Set<String> ids = new TreeSet<>();
        for (String key : keys) {
            String moduleId = moduleIdOf(key);
            if (moduleId != null) {
                ids.add(moduleId);
            }
        }
        return Set.copyOf(ids);
    }

    /**
     * @param moduleId a module id, as returned by {@link #affectedModuleIds}
     * @param keys     the same key set {@link #affectedModuleIds} was computed from
     * @return every key in {@code keys} that belongs to {@code moduleId} - for the "changed keys"
     *         part of {@link ConfigChangeHandler}'s log lines and for reverting exactly that
     *         module's part of a rejected change
     */
    static Set<String> keysForModule(String moduleId, Set<String> keys) {
        String prefix = moduleId + ".";
        Set<String> result = new TreeSet<>();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        return Set.copyOf(result);
    }

    private static String prefixOf(String key) {
        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(0, dot);
    }
}
