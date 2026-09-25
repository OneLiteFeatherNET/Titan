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

/**
 * A self-contained lobby feature.
 *
 * <p>A module owns exactly one package under {@code app/feature} and only touches the platform
 * through the {@link ModuleContext} handed to {@link #enable(ModuleContext)}. It never depends on
 * another module directly - shared behaviour goes through the platform (this package) or a common
 * library outside {@code app/feature} instead. See
 * {@code openspec/changes/lobby-feature-modules/design.md}, decision 2.
 *
 * <p>{@link ModuleRegistry} owns the lifecycle: it calls {@link #enable(ModuleContext)} once, in
 * registration order, before any player can reach the lobby, and later {@link #disable()} once, in
 * the reverse order - after the module's context has already been torn down (event node detached,
 * tasks cancelled, commands unregistered). A module therefore never has to remember what it
 * registered in order to clean up after itself.
 */
public interface LobbyModule {

    /**
     * A short, stable identifier for this module, used as the name of its own event node
     * ({@code "titan/" + id()}) and as the SLF4J MDC {@code module} value attached to a failing
     * listener.
     *
     * @return the module id, e.g. {@code "sit"}
     */
    String id();

    /**
     * Starts the module: registers listeners, tasks, commands and whatever else the module needs
     * through {@code context}. Called exactly once, before any player can reach the lobby.
     *
     * <p>{@code context} only accepts new listeners while this method runs; see
     * {@link ModuleContext#listen}.
     *
     * @param context this module's own handle to the platform
     */
    void enable(ModuleContext context);

    /**
     * Runs the module's own shutdown logic, after {@link ModuleRegistry} has already detached this
     * module's event node, cancelled its tasks and run every cleanup hook it registered through its
     * {@link ModuleContext} (unregistering commands, for instance). Most modules need no override.
     */
    default void disable() {
    }
}
