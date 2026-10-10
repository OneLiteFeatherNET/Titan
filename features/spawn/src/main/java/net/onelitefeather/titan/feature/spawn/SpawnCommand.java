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

import java.util.Objects;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.core.module.SpawnReturn;

/**
 * {@code /spawn}: brings the player back to the lobby spawn. Deliberately without a condition,
 * every player may use it; it only forwards to {@link SpawnReturn}.
 */
final class SpawnCommand extends Command {

    static final String NAME = "spawn";

    /** Operator-facing, the console has no locale and no position. */
    static final String CONSOLE_MESSAGE = "/spawn can only be used by players.";

    SpawnCommand(SpawnReturn spawnReturn) {
        super(NAME);
        Objects.requireNonNull(spawnReturn, "spawnReturn");
        setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                spawnReturn.sendToSpawnAndTell(player, SpawnReturn.Source.COMMAND);
            } else {
                sender.sendMessage(CONSOLE_MESSAGE);
            }
        });
    }
}
