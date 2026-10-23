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
package net.onelitefeather.titan.feature.dance;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.command.CommandManager;

/**
 * Registers {@code /tanz} with the platform's {@link CommandManager} on startup and removes it on
 * shutdown, so the command lives and dies with the column, as the admin commands do.
 */
@Singleton
public final class DanceCommands {

    private final CommandManager commandManager;
    private final DanceCommand command;

    public DanceCommands(CommandManager commandManager, DanceModule module) {
        this.commandManager = Objects.requireNonNull(commandManager, "commandManager must not be null");
        this.command = new DanceCommand(Objects.requireNonNull(module, "module must not be null"));
    }

    @PostConstruct
    void start() {
        this.commandManager.register(this.command);
    }

    @PreDestroy
    void stop() {
        this.commandManager.unregister(this.command);
    }
}
