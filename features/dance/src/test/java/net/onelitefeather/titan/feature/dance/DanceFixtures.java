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

import java.time.Clock;
import java.util.function.Function;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;

/** Builds the dance column the way the tests need it: a fresh module per test, real events. */
final class DanceFixtures {

    private static final Pos OPEN_AIR = new Pos(0, 64, 0);

    private DanceFixtures() {
    }

    static DanceModule module(Env env, TestTitanNode titan, PermissionService permissions, Clock clock, Telemetry telemetry) {
        return module(env, titan, permissions, clock, telemetry, MinestomDanceEffects::new);
    }

    static DanceModule module(Env env, TestTitanNode titan, PermissionService permissions, Clock clock, Telemetry telemetry, Function<Player, DanceEffects> effects) {
        return new DanceModule(titan.node(), permissions, env.process().scheduler(), clock, telemetry, effects);
    }

    /** A dance button click, as the client sends it. */
    static PlayerCustomClickEvent danceClick(Player player, Dance dance) {
        return new PlayerCustomClickEvent(player, DanceKeys.DANCE, DancePayload.of(dance));
    }

    static PlayerCustomClickEvent stopClick(Player player) {
        return new PlayerCustomClickEvent(player, DanceKeys.STOP, null);
    }

    static PlayerCustomClickEvent openClick(Player player) {
        return new PlayerCustomClickEvent(player, DanceKeys.OPEN, null);
    }

    /** A player on a real test connection, with his camera packets tracked from now on. */
    static Dancer connect(Env env, Instance instance) {
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        // Above the flat floor: a pose only sticks where the player's body fits.
        player.teleport(OPEN_AIR);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        return new Dancer(player, cameras);
    }

    /** The number of camera marker entities currently in {@code instance}. */
    static long markers(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.MARKER).count();
    }

    /** A player and the camera packets sent to him. */
    record Dancer(Player player, Collector<CameraPacket> cameras) {
    }
}
