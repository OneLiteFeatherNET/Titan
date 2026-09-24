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

import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;

/**
 * Default {@link ModuleCommands}. Registration and its matching unregistration are one unit: every
 * {@link #register} call immediately queues its own cleanup on the owning {@link ModuleContext} via
 * {@link ModuleContext#onDisable(Runnable)}, so {@link ModuleRegistry} does not need to know
 * anything about commands specifically - the same pattern a later item registry or navigator entry
 * registrar can reuse.
 */
final class ModuleCommandsImpl implements ModuleCommands {

    private final CommandManager commandManager;
    private final ModuleContext context;

    ModuleCommandsImpl(CommandManager commandManager, ModuleContext context) {
        this.commandManager = commandManager;
        this.context = context;
    }

    @Override
    public void register(Command command) {
        this.commandManager.register(command);
        this.context.onDisable(() -> this.commandManager.unregister(command));
    }
}
