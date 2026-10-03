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
package net.onelitefeather.titan.feature.spawn;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.command.CommandManager;
import net.onelitefeather.titan.core.module.SpawnReturn;

/**
 * Registers {@code /spawn} with the platform's {@link CommandManager} on startup and removes it on
 * shutdown, so the command lives and dies with this column.
 */
@Singleton
final class SpawnCommands {

    private final CommandManager commandManager;
    private final SpawnCommand spawnCommand;

    SpawnCommands(CommandManager commandManager, SpawnReturn spawnReturn) {
        this.commandManager = Objects.requireNonNull(commandManager, "commandManager");
        this.spawnCommand = new SpawnCommand(spawnReturn);
    }

    @PostConstruct
    void start() {
        this.commandManager.register(this.spawnCommand);
    }

    @PreDestroy
    void stop() {
        this.commandManager.unregister(this.spawnCommand);
    }
}
