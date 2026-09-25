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

import java.util.List;
import java.util.Set;

/**
 * What one {@link ConfigReloader#reload()} run produced - what the reload command (wired in a later
 * wave) translates into its player-facing reply.
 */
public sealed interface ReloadResult {

    /**
     * The fresh configuration snapshot was identical to the live one; nothing was applied and no
     * module was restarted.
     */
    record Unchanged() implements ReloadResult {
    }

    /**
     * A diff was applied. A module can end up in exactly one of the three lists below - never more
     * than one - depending on how its restart with the new values went.
     *
     * @param restartedModules modules that restarted successfully with their new configuration
     *                         values, in the order they were restarted
     * @param rejected         modules whose new values were rejected; each entry names the module,
     *                         the changed keys that were reverted and why the restart with the new
     *                         values failed - the module kept running with its previous values
     * @param disabledModules  modules that could not be restarted even with their previous values
     *                         put back, and are therefore disabled - an operator must intervene
     * @param flagsChanged     {@code true} if at least one {@code features.*} key changed; a flag
     *                         change never restarts a module
     */
    record Applied(
                   List<String> restartedModules,
                   List<RejectedModule> rejected,
                   List<String> disabledModules,
                   boolean flagsChanged) implements ReloadResult {

        public Applied {
            restartedModules = List.copyOf(restartedModules);
            rejected = List.copyOf(rejected);
            disabledModules = List.copyOf(disabledModules);
        }
    }

    /**
     * Building the fresh configuration snapshot failed; nothing was applied.
     *
     * @param file   the configuration file the problem was found in
     * @param detail a human-readable location of the problem within {@code file}
     */
    record Failed(String file, String detail) implements ReloadResult {
    }

    /**
     * One module whose new configuration values were rejected during an {@link Applied} run.
     *
     * @param moduleId the module id
     * @param keys     the module's changed, added or removed keys this reload attempted to apply
     * @param reason   why the restart with the new values failed
     */
    record RejectedModule(String moduleId, Set<String> keys, String reason) {

        public RejectedModule {
            keys = Set.copyOf(keys);
        }
    }
}
