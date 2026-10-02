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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** A player who comes into view of a run that is already going sees it like everyone else. */
@ExtendWith(MicrotusExtension.class)
class JumprunLateViewerTest {

    private static final Pos NEAR = StartedRun.STAND.add(0, 0, 8);

    /** Far outside the view distance of the run. */
    private static final Pos FAR = StartedRun.STAND.add(0, 0, 400);

    private static List<Entity> entitiesOf(Instance instance, EntityType type) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == type).toList();
    }

    @Test
    void aBystanderWhoArrivesLaterSeesTheDisplaysAndLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            StartedRun.start(env, fixture, instance, StartedRun.STAND);
            TestConnection connection = env.createConnection();
            Collector<SpawnEntityPacket> spawns = connection.trackIncoming(SpawnEntityPacket.class);
            Collector<SetPassengersPacket> passengers = connection.trackIncoming(SetPassengersPacket.class);

            Player late = connection.connect(instance, NEAR);

            List<Entity> displays = JumprunFixture.blockDisplays(instance);
            Entity label = entitiesOf(instance, EntityType.TEXT_DISPLAY).getFirst();
            assertEquals(2, displays.size(), "the run shows two blocks");
            assertTrue(displays.stream().allMatch(display -> display.getViewers().contains(late)), "the late player sees every block");
            assertTrue(label.getViewers().contains(late), "the late player sees the label");
            assertEquals(2, spawns.collect().stream().filter(spawn -> spawn.type() == EntityType.BLOCK_DISPLAY).count(), "the spawn of both blocks was sent");
            assertTrue(passengers.collect().stream().anyMatch(packet -> packet.passengersId().contains(label.getEntityId())), "the label was sent as a passenger of the runner");
        }
    }

    @Test
    void aBystanderWhoTeleportsNextToTheRunnerSeesTheDisplaysAndLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            StartedRun.start(env, fixture, instance, StartedRun.STAND);
            Player late = env.createConnection().connect(instance, FAR);
            assertTrue(entitiesOf(instance, EntityType.TEXT_DISPLAY).getFirst().getViewers().isEmpty(), "out of view at first");

            late.teleport(NEAR).join();
            env.tick();

            assertTrue(JumprunFixture.blockDisplays(instance).stream().allMatch(display -> display.getViewers().contains(late)), "the blocks come into view");
            assertTrue(entitiesOf(instance, EntityType.TEXT_DISPLAY).getFirst().getViewers().contains(late), "the label comes into view");
        }
    }
}
