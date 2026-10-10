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

import java.util.concurrent.Executor;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stops the service cleanly via {@link net.minestom.server.MinecraftServer#stopCleanly()}, which
 * CloudNet triggers by writing {@code stop} to console. The console may always stop it; a player
 * needs {@code titan.command.stop}.
 */
final class StopCommand extends Command {

    private static final String NAME = "stop";
    private static final String PERMISSION = "titan.command.stop";

    /**
     * @param stopThread the executor that runs {@code shutdown}; the span ends once it has
     *                   accepted the task, before the shutdown runs
     * @param shutdown   what stops the service
     */
    StopCommand(AdminTelemetry telemetry, Executor stopThread, Runnable shutdown) {
        super(NAME);
        setCondition(telemetry.guard(NAME, this::canStop));
        // Runs on a separate thread: stopCleanly() shuts down the console thread that
        // triggered this, so it must not run there.
        setDefaultExecutor((sender, context) -> {
            telemetry.executed(NAME, sender);
            stopThread.execute(shutdown);
        });
    }

    private boolean canStop(@NotNull CommandSender sender, @Nullable String commandString) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE)).test(PERMISSION);
    }
}
