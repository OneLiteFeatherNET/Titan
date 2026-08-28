/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
