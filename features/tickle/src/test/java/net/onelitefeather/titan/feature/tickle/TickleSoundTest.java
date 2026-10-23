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
package net.onelitefeather.titan.feature.tickle;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The tickle sound as heard on the wire. Each test builds its own {@code Env}, players and module;
 * the clock is fixed and the pitch comes from a seeded random (F.I.R.S.T.).
 */
@ExtendWith(MicrotusExtension.class)
class TickleSoundTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Pos ATTACKER = new Pos(0, 40, 0);
    private static final Pos TARGET = new Pos(2, 40, 0);
    /** Five blocks from the target: inside the reach. */
    private static final Pos NEARBY = new Pos(7, 40, 0);
    /** Thirty blocks from the target: outside the reach. */
    private static final Pos FAR = new Pos(32, 40, 0);

    private static final class Scene {
        final Player attacker;
        final Player target;
        final TestConnection attackerConnection;
        final TestConnection targetConnection;
        final TestConnection nearbyConnection;
        final TestConnection farConnection;
        final TestTitanNode titanNode;
        final TickleModule module;

        Scene(Env env) {
            Instance instance = env.createFlatInstance();
            this.attackerConnection = env.createConnection();
            this.attacker = this.attackerConnection.connect(instance, ATTACKER);
            this.targetConnection = env.createConnection();
            this.target = this.targetConnection.connect(instance, TARGET);
            this.nearbyConnection = env.createConnection();
            this.nearbyConnection.connect(instance, NEARBY);
            this.farConnection = env.createConnection();
            this.farConnection.connect(instance, FAR);
            this.attacker.setItemInOffHand(ItemStack.of(Material.FEATHER));
            this.titanNode = TestTitanNode.attach(env);
            EventNode<Event> titan = this.titanNode.node();
            this.module = new TickleModule(titan, Clock.fixed(NOW, ZoneOffset.UTC), Telemetry.noop(), new Random(42));
            this.module.start();
        }

        void tickle(Env env) {
            env.process().eventHandler().call(new EntityAttackEvent(this.attacker, this.target));
            env.tick();
        }

        void close() {
            this.module.stop();
            this.titanNode.close();
        }
    }

    private static List<SoundEffectPacket> tickleSounds(Collector<SoundEffectPacket> collector) {
        return collector.collect().stream().filter(packet -> packet.soundEvent() == SoundEvent.ENTITY_ALLAY_ITEM_GIVEN).toList();
    }

    @DisplayName("A tickle sound reaches the tickler, the target and a nearby player")
    @Test
    void tickleSoundReachesTicklerTargetAndNearbyPlayer(Env env) {
        Scene scene = new Scene(env);
        try {
            Collector<SoundEffectPacket> attackerSounds = scene.attackerConnection.trackIncoming(SoundEffectPacket.class);
            Collector<SoundEffectPacket> targetSounds = scene.targetConnection.trackIncoming(SoundEffectPacket.class);
            Collector<SoundEffectPacket> nearbySounds = scene.nearbyConnection.trackIncoming(SoundEffectPacket.class);

            scene.tickle(env);

            Assertions.assertEquals(1, tickleSounds(attackerSounds).size(), "the tickler hears the sound once");
            Assertions.assertEquals(1, tickleSounds(targetSounds).size(), "the tickled player hears the sound once");
            Assertions.assertEquals(1, tickleSounds(nearbySounds).size(), "a player 5 blocks from the target hears the sound once");
        } finally {
            scene.close();
        }
    }

    @DisplayName("A player 30 blocks away does not get the tickle sound")
    @Test
    void farPlayerGetsNoTickleSound(Env env) {
        Scene scene = new Scene(env);
        try {
            Collector<SoundEffectPacket> farSounds = scene.farConnection.trackIncoming(SoundEffectPacket.class);

            scene.tickle(env);

            Assertions.assertTrue(tickleSounds(farSounds).isEmpty(), "a player beyond the reach must not receive the packet");
        } finally {
            scene.close();
        }
    }

    @DisplayName("The tickle sound pitch lies between 0.9 and 1.2")
    @Test
    void tickleSoundPitchIsWithinRange(Env env) {
        Scene scene = new Scene(env);
        try {
            Collector<SoundEffectPacket> targetSounds = scene.targetConnection.trackIncoming(SoundEffectPacket.class);

            scene.tickle(env);

            float pitch = tickleSounds(targetSounds).getFirst().pitch();
            Assertions.assertTrue(pitch >= TickleSounds.MIN_PITCH && pitch < TickleSounds.MAX_PITCH, "pitch " + pitch + " outside the configured range");
        } finally {
            scene.close();
        }
    }

    @DisplayName("Attacking without a feather plays no tickle sound")
    @Test
    void attackWithoutFeatherPlaysNoTickleSound(Env env) {
        Scene scene = new Scene(env);
        try {
            scene.attacker.setItemInOffHand(ItemStack.of(Material.STICK));
            Collector<SoundEffectPacket> targetSounds = scene.targetConnection.trackIncoming(SoundEffectPacket.class);

            scene.tickle(env);

            Assertions.assertTrue(tickleSounds(targetSounds).isEmpty(), "no feather, no tickle, no sound");
        } finally {
            scene.close();
        }
    }
}
