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
package net.onelitefeather.titan.feature.npc;

import java.util.List;
import java.util.Random;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.ambience.AmbienceZone;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Every test gets its own {@code Env}, lobby instance, titan node, module and random seed. */
@ExtendWith(MicrotusExtension.class)
class AmbienceModuleTest {

    private static final Box AREA = new Box(new Vec(0, 30, 0), new Vec(20, 50, 20));
    private static final Vec APPARITION = new Vec(10, 40, 10);
    private static final Pos NEAR = new Pos(10.5, 40, 4.5);
    private static final Pos OUTSIDE = new Pos(60.5, 40, 60.5);
    private static final int TAKTS = 12;

    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void closeTelemetry() {
        this.testTelemetry.close();
    }

    private static AmbienceZone zone(AmbienceZone.Particles particles, AmbienceZone.Sounds sounds, List<Vec> apparitions) {
        return new AmbienceZone("tunnel", AREA, particles, sounds, List.of(), List.of(), apparitions, null);
    }

    private AmbienceModule start(Env env, TestTitanNode titan, Instance lobby, AmbienceZone zone) {
        AmbienceModule module = new AmbienceModule(titan.node(), lobby, env.process().scheduler(), () -> List.of(zone), this.testTelemetry.telemetry(), new Random(5));
        module.start();
        return module;
    }

    private static void ticks(Env env, int count) {
        for (int tick = 0; tick < count; tick++) {
            env.tick();
        }
    }

    private static List<Entity> silhouettes(Instance lobby) {
        return lobby.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.MANNEQUIN).toList();
    }

    @DisplayName("A silhouette appears only to the player who is near the point")
    @Test
    void silhouetteIsShownToOnePlayer(Env env) {
        Instance lobby = env.createFlatInstance();
        Player near = env.createPlayer(lobby, NEAR);
        Player far = env.createPlayer(lobby, OUTSIDE);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, zone(null, null, List.of(APPARITION)));
            ticks(env, TAKTS);
            List<Entity> figures = silhouettes(lobby);
            Assertions.assertEquals(1, figures.size(), "one silhouette exists for the player who is near");
            Assertions.assertTrue(figures.getFirst().getViewers().contains(near), "the near player sees the silhouette");
            Assertions.assertFalse(figures.getFirst().getViewers().contains(far), "the player outside the zone does not");
        }
    }

    @DisplayName("A silhouette is removed when the player comes within 4 blocks of it")
    @Test
    void silhouetteIsRemovedWhenApproached(Env env) {
        Instance lobby = env.createFlatInstance();
        Player near = env.createPlayer(lobby, NEAR);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, zone(null, null, List.of(APPARITION)));
            ticks(env, TAKTS);
            near.teleport(new Pos(10.5, 40, 7.5)).join();
            ticks(env, TAKTS);
            Assertions.assertTrue(silhouettes(lobby).isEmpty(), "within 4 blocks the silhouette is gone");
        }
    }

    @DisplayName("A disconnect removes the silhouette the player still sees")
    @Test
    void disconnectRemovesSilhouette(Env env) {
        Instance lobby = env.createFlatInstance();
        Player near = env.createPlayer(lobby, NEAR);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, zone(null, null, List.of(APPARITION)));
            ticks(env, TAKTS);
            Assertions.assertEquals(1, silhouettes(lobby).size(), "precondition: the silhouette is shown before the player leaves");
            env.process().eventHandler().call(new PlayerDisconnectEvent(near));
            ticks(env, TAKTS);
            Assertions.assertTrue(silhouettes(lobby).isEmpty(), "a silhouette of a player who left is not kept");
        }
    }

    @DisplayName("Ambient sounds are played only to players inside the zone")
    @Test
    void soundsGoOnlyInsideTheZone(Env env) {
        Instance lobby = env.createFlatInstance();
        TestConnection insideConnection = env.createConnection();
        TestConnection outsideConnection = env.createConnection();
        Collector<ServerPacket> insideSent = insideConnection.trackIncoming();
        Collector<ServerPacket> outsideSent = outsideConnection.trackIncoming();
        insideConnection.connect(lobby, NEAR);
        outsideConnection.connect(lobby, OUTSIDE);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, zone(null, new AmbienceZone.Sounds("ambient.cave", 1, 1), List.of()));
            ticks(env, 60);
            Assertions.assertTrue(insideSent.collect().stream().anyMatch(packet -> packet.getClass().getSimpleName().contains("Sound")), "the player inside the zone hears the ambient sound");
            Assertions.assertTrue(outsideSent.collect().stream().noneMatch(packet -> packet.getClass().getSimpleName().contains("Sound")), "the player outside hears nothing");
        }
    }

    @DisplayName("Particles are sent only to players in or near the zone")
    @Test
    void particlesGoOnlyNearTheZone(Env env) {
        Instance lobby = env.createFlatInstance();
        TestConnection nearConnection = env.createConnection();
        TestConnection farConnection = env.createConnection();
        Collector<ServerPacket> nearSent = nearConnection.trackIncoming();
        Collector<ServerPacket> farSent = farConnection.trackIncoming();
        nearConnection.connect(lobby, NEAR);
        farConnection.connect(lobby, OUTSIDE);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, zone(new AmbienceZone.Particles("white_ash", 3), null, List.of()));
            ticks(env, TAKTS);
            Assertions.assertTrue(nearSent.collect().stream().anyMatch(packet -> packet instanceof ParticlePacket), "the player near the zone receives its particles");
            Assertions.assertTrue(farSent.collect().stream().noneMatch(packet -> packet instanceof ParticlePacket), "the player far away receives none");
        }
    }
}
