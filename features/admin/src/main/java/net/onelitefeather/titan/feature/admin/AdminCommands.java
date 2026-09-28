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
package net.onelitefeather.titan.feature.admin;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.command.CommandManager;

/**
 * Registers {@code stop} and {@code end} with the platform's {@link CommandManager} on startup
 * and unregisters both on shutdown, so the two admin commands live and die with this column's own
 * bean instead of being wired by hand in the composition root.
 */
@Singleton
public final class AdminCommands {

    private final CommandManager commandManager;
    private final StopCommand stopCommand = new StopCommand();
    private final EndCommand endCommand = new EndCommand();

    public AdminCommands(CommandManager commandManager) {
        this.commandManager = Objects.requireNonNull(commandManager, "commandManager must not be null");
    }

    @PostConstruct
    void start() {
        this.commandManager.register(this.stopCommand);
        this.commandManager.register(this.endCommand);
    }

    @PreDestroy
    void stop() {
        this.commandManager.unregister(this.stopCommand);
        this.commandManager.unregister(this.endCommand);
    }
}
