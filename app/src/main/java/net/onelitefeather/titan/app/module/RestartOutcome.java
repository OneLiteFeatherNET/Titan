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
package net.onelitefeather.titan.app.module;

import java.util.Objects;

/**
 * The outcome of {@link ModuleRegistry#restart(String)}.
 *
 * <p>Sealed to exactly the two shapes a restart can end in - see {@code design.md}, decision 3:
 * <ul>
 * <li>{@link Restarted}: the module is running again with a fresh {@link ModuleContext}, the
 * shared {@link net.onelitefeather.titan.app.module.item.ItemRegistry} and
 * {@link net.onelitefeather.titan.app.module.navigator.NavigatorEntries} have re-validated,
 * and every online player has been re-equipped.</li>
 * <li>{@link Failed}: the module's own {@code enable} threw, or that same validation rejected the
 * result once every module was back up. Either way the partial start was torn down the same
 * way {@link ModuleRegistry#disableAll()} tears a module down, so nothing from the failed
 * attempt - no listener, task or item - is left behind.</li>
 * </ul>
 *
 * <p>Neither case ever throws out of {@link ModuleRegistry#restart(String)}: a later
 * {@code ConfigReloader} is the caller that decides what {@link Failed#cause()} means - typically
 * restoring the module's previous values in the configuration facade and calling
 * {@link ModuleRegistry#restart(String)} again.
 */
public sealed interface RestartOutcome {

    /**
     * The module is running again with its new configuration.
     */
    record Restarted() implements RestartOutcome {
    }

    /**
     * The module could not be restarted and is left disabled.
     *
     * @param cause the failure - the module's own {@code enable} exception, or the exception the
     *              shared item registry or navigator entries validation threw once every module was
     *              back up
     */
    record Failed(Throwable cause) implements RestartOutcome {

        public Failed {
            Objects.requireNonNull(cause, "cause must not be null");
        }
    }
}
