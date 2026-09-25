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

import net.minestom.server.command.builder.Command;

/**
 * A module's own view of the command manager, obtained through {@link ModuleContext#commands()}.
 *
 * <p>A command registered through this interface is unregistered automatically when the owning
 * module is disabled - a module never has to remember what it registered or unregister it by hand.
 */
public interface ModuleCommands {

    /**
     * Registers {@code command}. It disappears again as soon as the owning module is disabled.
     *
     * @param command the command to register
     */
    void register(Command command);
}
