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

/**
 * What {@link ModuleRestarter#restart(String)} reports for one module's restart attempt.
 *
 * <p>Deliberately its own small sealed type, not the {@code RestartOutcome}
 * {@code net.onelitefeather.titan.app.module.ModuleRegistry#restart(String)} returns (added in a
 * parallel wave of this change): the reload core here must not depend on the {@code module}
 * package,
 * so the wiring wave that connects the two adapts {@code ModuleRegistry}'s outcome to this one with
 * a
 * small lambda instead.
 */
public sealed interface ModuleRestartOutcome {

    /**
     * The module shut down and started back up with its new configuration values without error.
     */
    record Restarted() implements ModuleRestartOutcome {
    }

    /**
     * The module could not start back up with its new configuration values.
     *
     * @param cause why the restart failed, e.g. a validation exception a module's config record
     *              threw
     */
    record Failed(Throwable cause) implements ModuleRestartOutcome {
    }
}
