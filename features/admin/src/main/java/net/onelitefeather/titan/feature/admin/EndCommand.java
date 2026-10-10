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

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class EndCommand extends Command {

    private static final String NAME = "end";

    EndCommand(AdminTelemetry telemetry, Runnable shutdown) {
        super(NAME);
        setCondition(telemetry.guard(NAME, this::hasPermission));
        addSyntax((commandSender, commandContext) -> execute(telemetry, commandSender, shutdown));
    }

    private static void execute(AdminTelemetry telemetry, CommandSender commandSender, Runnable shutdown) {
        telemetry.executed(NAME, commandSender);
        shutdown.run();
    }

    private boolean hasPermission(@NotNull CommandSender commandSender, @Nullable String s) {
        return false;
        // return commandSender.hasPermission("titan.command.end") ||
        // commandSender.hasPermission("lobby.end");
    }
}
