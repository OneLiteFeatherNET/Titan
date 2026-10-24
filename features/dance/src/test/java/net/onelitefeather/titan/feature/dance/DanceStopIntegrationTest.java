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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Every way a dance ends: the dancer is standing again, has his own view back, and neither the
 * camera entity nor the NPC is left in the instance.
 */
@ExtendWith(MicrotusExtension.class)
class DanceStopIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final PermissionService NO_PERMISSIONS = new FakePermissionService();

    private static DanceModule started(Env env, TestTitanNode titan, DanceFixtures.Dancer dancer, Function<LivingEntity, DanceEffects> effects) {
        // Another player in the instance: the real dancer must be hidden from him during the dance.
        env.createPlayer(dancer.player().getInstance());
        DanceModule module = DanceFixtures.module(env, titan, NO_PERMISSIONS, new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop(), effects);
        module.start();
        env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.GROOVE));
        env.tick();
        return module;
    }

    private static DanceModule started(Env env, TestTitanNode titan, DanceFixtures.Dancer dancer) {
        return started(env, titan, dancer, MinestomDanceEffects::new);
    }

    private static void assertStoppedAndRestored(Instance instance, DanceFixtures.Dancer dancer, String cause) {
        Assertions.assertEquals(0, DanceFixtures.cameras(instance), cause + ": the camera entity must be removed");
        Assertions.assertEquals(0, DanceFixtures.npcs(instance), cause + ": the NPC must be removed");
        List<Player> others = instance.getPlayers().stream().filter(player -> player != dancer.player()).toList();
        Assertions.assertFalse(others.isEmpty(), cause + ": the test needs another player in the instance");
        for (Player other : others) {
            Assertions.assertTrue(dancer.player().getViewers().contains(other), cause + ": the other players must see the real dancer again");
        }
        Assertions.assertEquals(EntityPose.STANDING, dancer.player().getPose(), cause + ": the real dancer must keep his own pose");
        List<CameraPacket> cameras = dancer.cameras().collect();
        Assertions.assertEquals(dancer.player().getEntityId(), cameras.getLast().cameraId(), cause + ": the last camera packet must give the dancer his own view back");
    }

    @DisplayName("Moving beyond the threshold ends the dance and gives the dancer his view back")
    @Test
    void movementEndsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                Pos moved = dancer.player().getPosition().add(1, 0, 0);
                env.process().eventHandler().call(new PlayerMoveEvent(dancer.player(), moved, true));

                assertStoppedAndRestored(instance, dancer, "a step of one block");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A drift within the threshold keeps the dance going")
    @Test
    void smallMovementKeepsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                Pos drifted = dancer.player().getPosition().add(0.1, 0, 0);
                env.process().eventHandler().call(new PlayerMoveEvent(dancer.player(), drifted, true));

                Assertions.assertEquals(1, DanceFixtures.cameras(instance), "a drift within the threshold must not end the dance");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A movement key pressed by the player ends the dance")
    @Test
    void movementInputEndsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                env.process().eventHandler().call(new PlayerPacketEvent(dancer.player(), new ClientInputPacket(true, false, false, false, false, false, false)));

                assertStoppedAndRestored(instance, dancer, "pressing forward");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Sneaking alone does not end the dance")
    @Test
    void sneakingDoesNotEndTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                env.process().eventHandler().call(new PlayerPacketEvent(dancer.player(), new ClientInputPacket(false, false, false, false, false, true, false)));

                Assertions.assertEquals(1, DanceFixtures.cameras(instance), "the groove's own sneak must not end the dance");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Attacking another player ends the dance")
    @Test
    void attackEndsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        Player other = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                env.process().eventHandler().call(new EntityAttackEvent(dancer.player(), other));

                assertStoppedAndRestored(instance, dancer, "an attack");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A block interaction ends the dance even if another feature has cancelled it")
    @Test
    void cancelledInteractionEndsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                PlayerBlockInteractEvent interaction = new PlayerBlockInteractEvent(dancer.player(), PlayerHand.MAIN, instance, Block.STONE, new BlockVec(0, 40, 0), new Vec(0.5, 1, 0.5), BlockFace.TOP);
                interaction.setCancelled(true);
                env.process().eventHandler().call(interaction);

                assertStoppedAndRestored(instance, dancer, "a cancelled block interaction");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The stop button ends the dance")
    @Test
    void stopButtonEndsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                env.process().eventHandler().call(DanceFixtures.stopClick(dancer.player()));

                assertStoppedAndRestored(instance, dancer, "the stop button");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Leaving the instance ends the dance")
    @Test
    void leavingTheInstanceEndsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                env.process().eventHandler().call(new RemoveEntityFromInstanceEvent(instance, dancer.player()));

                assertStoppedAndRestored(instance, dancer, "leaving the instance");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A disconnect ends the dance and removes the camera entity and the NPC")
    @Test
    void disconnectRemovesTheCamera(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                env.process().eventHandler().call(new PlayerDisconnectEvent(dancer.player()));

                assertStoppedAndRestored(instance, dancer, "a disconnect");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A dance that runs to its end restores the dancer and removes the camera and the NPC")
    @Test
    void finishedDanceRestoresTheDancer(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            try {
                for (int tick = 0; tick <= Dance.GROOVE.sequence().lengthTicks(); tick++) {
                    env.tick();
                }

                assertStoppedAndRestored(instance, dancer, "the end of the groove");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A failing dance step still gives the dancer his view back and removes the camera and the NPC")
    @Test
    void failingStepStillRestoresTheCamera(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        Function<LivingEntity, DanceEffects> failing = npc -> new DanceEffects() {
            @Override
            public void apply(DanceAction action, Pos anchor) {
                throw new IllegalStateException("a step failed");
            }

            @Override
            public void reset(Pos anchor) {
                new MinestomDanceEffects(npc).reset(anchor);
            }
        };

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer, failing);
            try {
                env.tick();

                assertStoppedAndRestored(instance, dancer, "a failing step");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A failing pose reset still removes the camera entity and the NPC")
    @Test
    void failingResetStillRemovesTheCamera(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        Function<LivingEntity, DanceEffects> failingReset = npc -> new DanceEffects() {
            @Override
            public void apply(DanceAction action, Pos anchor) {
                new MinestomDanceEffects(npc).apply(action, anchor);
            }

            @Override
            public void reset(Pos anchor) {
                throw new IllegalStateException("the reset failed");
            }
        };

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer, failingReset);
            module.stop();

            Assertions.assertEquals(0, DanceFixtures.cameras(instance), "a reset that throws must not leave the camera entity behind");
            Assertions.assertEquals(0, DanceFixtures.npcs(instance), "a reset that throws must not leave the NPC behind");
            Assertions.assertEquals(dancer.player().getEntityId(), dancer.cameras().collect().getLast().cameraId(), "a reset that throws must still give the dancer his view back");
            Assertions.assertTrue(instance.getPlayers().stream().filter(player -> player != dancer.player()).allMatch(player -> dancer.player().getViewers().contains(player)), "a reset that throws must still show the real dancer to the others");
        }
    }

    @DisplayName("Shutting the column down ends a running dance and gives the view back")
    @Test
    void shutdownEndsTheDance(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = started(env, titan, dancer);
            module.stop();

            assertStoppedAndRestored(instance, dancer, "the shutdown");
        }
    }

    @DisplayName("After shutdown no dance click starts a dance any more")
    @Test
    void stoppedColumnStartsNothing(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, NO_PERMISSIONS, new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            module.stop();

            env.process().eventHandler().call(new PlayerCustomClickEvent(dancer.player(), DanceKeys.DANCE, CompoundBinaryTag.builder().putString("id", Dance.WAVE.id()).build()));

            Assertions.assertEquals(0, DanceFixtures.cameras(instance), "a stopped column must not react to a dance click");
            Assertions.assertEquals(0, DanceFixtures.npcs(instance), "a stopped column must not spawn an NPC");
        }
    }
}
