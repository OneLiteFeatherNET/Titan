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
import java.util.List;
import java.util.function.Function;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;

/** Builds the dance column the way the tests need it: a fresh module per test, real events. */
final class DanceFixtures {

    private static final Pos OPEN_AIR = new Pos(0, 64, 0);

    /** The top of the flat floor: the NPC stands here, and gravity holds it up. */
    static final Pos FLOOR = new Pos(0, 40, 0);

    private DanceFixtures() {
    }

    static DanceModule module(Env env, TestTitanNode titan, PermissionService permissions, Clock clock, Telemetry telemetry) {
        return module(env, titan, permissions, clock, telemetry, MinestomDanceEffects::new);
    }

    static DanceModule module(Env env, TestTitanNode titan, PermissionService permissions, Clock clock, Telemetry telemetry, Function<LivingEntity, DanceEffects> effects) {
        return new DanceModule(titan.node(), permissions, env.process().scheduler(), clock, telemetry, effects);
    }

    /** A dance button click, as the client sends it. */
    static PlayerCustomClickEvent danceClick(Player player, Dance dance) {
        return new PlayerCustomClickEvent(player, DanceKeys.DANCE, DancePayload.of(dance));
    }

    static PlayerCustomClickEvent stopClick(Player player) {
        return new PlayerCustomClickEvent(player, DanceKeys.STOP, null);
    }

    /**
     * A player on a real test connection, with his camera and animation packets tracked from now
     * on.
     */
    static Dancer connect(Env env, Instance instance) {
        return connect(env, instance, OPEN_AIR);
    }

    /** A player standing at {@code spot}, with his camera and animation packets tracked. */
    static Dancer connect(Env env, Instance instance, Pos spot) {
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.teleport(spot);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        Collector<EntityAnimationPacket> animations = connection.trackIncoming(EntityAnimationPacket.class);
        return new Dancer(player, cameras, animations);
    }

    /** The number of camera entities currently in {@code instance}. */
    static long cameras(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.ITEM_DISPLAY).count();
    }

    /** The number of NPCs currently in {@code instance}. */
    static long npcs(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.MANNEQUIN).count();
    }

    /**
     * The NPC of the single dance running in {@code instance}; fails if there is not exactly one.
     */
    static LivingEntity npc(Instance instance) {
        List<Entity> npcs = instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.MANNEQUIN).toList();
        if (npcs.size() != 1) {
            throw new AssertionError("expected exactly one NPC in the instance, found " + npcs.size());
        }
        return (LivingEntity) npcs.getFirst();
    }

    /**
     * The camera entity the dancer's last camera packet points at, or {@code null} if it is the
     * dancer himself.
     */
    static Entity cameraOf(Instance instance, Dancer dancer) {
        int cameraId = dancer.cameras().collect().getLast().cameraId();
        return instance.getEntities().stream().filter(entity -> entity.getEntityId() == cameraId).findFirst().orElse(null);
    }

    /** A player and the packets sent to him that the dance tests look at. */
    record Dancer(Player player, Collector<CameraPacket> cameras,
                  Collector<EntityAnimationPacket> animations) {
    }
}
