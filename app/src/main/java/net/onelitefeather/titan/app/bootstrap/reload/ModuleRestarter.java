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

/**
 * Restarts exactly one lobby module by id, on the tick thread, so it picks up its section of a just
 * applied configuration diff.
 *
 * <p>The production adapter (wired in a later wave) delegates to
 * {@code net.onelitefeather.titan.app.module.ModuleRegistry#restart(String)} and maps its own
 * sealed
 * {@code RestartOutcome} to {@link ModuleRestartOutcome} with a small lambda - see that type's
 * javadoc for why {@link ConfigReloader} does not depend on the {@code module} package directly.
 * {@link #moduleOrder()} is backed the same way, by
 * {@code net.onelitefeather.titan.app.module.ModuleRegistry#moduleIds()} - so this interface, not
 * the {@code module} package, is the only ordering seam {@link ConfigReloader} knows about.
 */
public interface ModuleRestarter {

    /**
     * @param moduleId the module to restart, e.g. {@code "sit"}
     * @return whether the restart succeeded with the module's new configuration values
     */
    ModuleRestartOutcome restart(String moduleId);

    /**
     * The ids of every module this restarter can restart, in <em>registration</em> order - not the
     * alphabetical order a {@link ConfigDiff}'s {@code affectedModuleIds} happens to iterate in.
     * See
     * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 3: restarts follow
     * registration order, so {@link ConfigReloader} restarts affected modules in the order this
     * method returns them, skipping any affected id this restarter does not know - a diff key whose
     * prefix never named a module (e.g. a typo, or a section such as {@code spawn} with no matching
     * module) is deliberately never passed to {@link #restart(String)}.
     *
     * @return every known module id, in registration order
     */
    List<String> moduleOrder();
}
