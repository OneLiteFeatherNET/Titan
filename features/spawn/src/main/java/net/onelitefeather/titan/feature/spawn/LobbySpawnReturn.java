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

import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.titan.core.module.LobbyReturnToSpawnEvent;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.module.SpawnReturn;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Brings a player back to the lobby spawn: features are told first through
 * {@link LobbyReturnToSpawnEvent} so they can clean up their state, then the player is teleported.
 */
@Singleton
final class LobbySpawnReturn implements SpawnReturn {

    private static final Logger LOGGER = LoggerFactory.getLogger(LobbySpawnReturn.class);

    private final LobbySpawn spawn;
    private final SpawnMessages messages;
    private final SpawnTelemetry telemetry;

    LobbySpawnReturn(LobbySpawn spawn, SpawnMessages messages, Telemetry telemetry) {
        this.spawn = Objects.requireNonNull(spawn, "spawn");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.telemetry = new SpawnTelemetry(Objects.requireNonNull(telemetry, "telemetry"));
    }

    @Override
    public Result sendToSpawn(Player player, Source source) {
        return this.telemetry.inReturn(player.getUuid(), source, () -> returnTo(player));
    }

    private Result returnTo(Player player) {
        Pos position = this.spawn.position();
        if (position == null) {
            LOGGER.atDebug().addKeyValue("player", player.getUuid()).log("no spawn point on the active map, player stays");
            return Result.NO_SPAWN;
        }
        EventDispatcher.call(new LobbyReturnToSpawnEvent(player));
        // Teleporting alone does not end a glide; without this the player keeps gliding from spawn.
        player.setFlyingWithElytra(false);
        // Teleporting keeps the velocity: a glider would fly on from spawn. The delta tells the client
        // to drop its momentum, the server-side reset is repeated once the (possibly chunk-loading)
        // teleport is done, so nothing set in between survives.
        player.setVelocity(Vec.ZERO);
        player.teleport(position, Vec.ZERO).whenComplete((done, cause) -> {
            if (cause == null) {
                player.setVelocity(Vec.ZERO);
            } else {
                LOGGER.atWarn().addKeyValue("player", player.getUuid()).setCause(cause).log("Could not teleport player to spawn");
            }
        });
        LOGGER.atDebug().addKeyValue("player", player.getUuid()).log("player returned to spawn");
        return Result.RETURNED;
    }

    @Override
    public void sendToSpawnAndTell(Player player, Source source) {
        Result result = sendToSpawn(player, source);
        player.sendMessage(switch (result) {
            case RETURNED -> this.messages.returned(player.getLocale());
            case NO_SPAWN -> this.messages.noSpawn(player.getLocale());
        });
    }
}
