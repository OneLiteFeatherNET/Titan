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
package net.onelitefeather.titan.app.commands;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stops the service cleanly. CloudNet shuts a service down by writing {@code stop} to its console
 * (see the console reader in {@code TitanApplication}); this command turns that into a clean,
 * fast {@link MinecraftServer#stopCleanly()} so the node does not have to kill the process after a
 * timeout.
 *
 * <p>The server console (any non-player sender, i.e. CloudNet) may always stop the service.
 * Players need the {@code titan.command.stop} permission.
 */
public final class StopCommand extends Command {

    private static final String PERMISSION = "titan.command.stop";

    public StopCommand() {
        super("stop");
        setCondition(this::canStop);
        // Stop on a separate platform thread so stopCleanly() (which shuts down the server, and
        // with it the console thread that triggered this) does not run on the caller's thread.
        setDefaultExecutor((sender, context) -> Thread.ofPlatform().name("titan-stop").start(() -> {
            MinecraftServer.stopCleanly();
            System.exit(0);
        }));
    }

    private boolean canStop(@NotNull CommandSender sender, @Nullable String commandString) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE)).test(PERMISSION);
    }
}
