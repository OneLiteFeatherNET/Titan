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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The score above the runner's head, which the other players see and the runner does not. */
@ExtendWith(MicrotusExtension.class)
class JumprunScoreLabelTest {

    private static final Pos BYSTANDER_STAND = StartedRun.STAND.add(0, 0, 8);

    private record Scene(StartedRun run, Player bystander, TestConnection bystanderConnection,
                         Instance instance) {

        static Scene start(Env env, JumprunFixture fixture) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection connection = env.createConnection();
            Player bystander = connection.connect(instance, BYSTANDER_STAND);
            return new Scene(StartedRun.start(env, fixture, instance, StartedRun.STAND), bystander, connection, instance);
        }

        List<Entity> labels() {
            return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.TEXT_DISPLAY).toList();
        }

        Entity label() {
            return labels().getFirst();
        }

        String text() {
            return PlainTextComponentSerializer.plainText().serialize(((TextDisplayMeta) label().getEntityMeta()).getText());
        }
    }

    @Test
    void theRunnerCarriesALabelFromTheStart(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            assertEquals(1, scene.labels().size(), "one label per run");
            assertTrue(scene.run().player().getPassengers().contains(scene.label()), "the label rides on the runner, so it follows every step");
        }
    }

    @Test
    void theLabelFacesEveryViewer(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            assertEquals(BillboardConstraints.CENTER, ((TextDisplayMeta) scene.label().getEntityMeta()).getBillboardRenderConstraints(), "the text turns towards the viewer");
        }
    }

    @Test
    void theBystanderSeesTheLabelAndTheRunnerDoesNot(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            assertTrue(scene.label().getViewers().contains(scene.bystander()), "the bystander sees it");
            assertFalse(scene.label().getViewers().contains(scene.run().player()), "the runner does not see his own label");
        }
    }

    @Test
    void theLabelShowsScoreZeroAtTheStart(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            assertTrue(scene.text().endsWith("Jump & Run · 0"), "the same text in every language");
        }
    }

    @Test
    void theLabelStaysAtScoreZeroThroughTheAscent(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);

            assertTrue(scene.text().endsWith("Jump & Run · 0"), "the ascent does not score");
        }
    }

    @Test
    void theLabelShowsTheScoreAfterEveryJump(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);

            for (int score = 1; score <= 3; score++) {
                scene.run().landOnNext();

                assertTrue(scene.text().endsWith("Jump & Run · " + score), "after scored jump " + score);
            }
        }
    }

    @Test
    void anUnchangedScoreSendsNoNewMetadata(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            int labelId = scene.label().getEntityId();
            Collector<EntityMetaDataPacket> metadata = scene.bystanderConnection().trackIncoming(EntityMetaDataPacket.class);

            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS - 1);

            assertTrue(metadata.collect().stream().noneMatch(packet -> packet.entityId() == labelId), "the ascent keeps the score at zero, so the label is not updated");
        }
    }

    @Test
    void aChangedScoreSendsNewMetadata(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS);
            int labelId = scene.label().getEntityId();
            Collector<EntityMetaDataPacket> metadata = scene.bystanderConnection().trackIncoming(EntityMetaDataPacket.class);

            scene.run().landOnNext();

            assertTrue(metadata.collect().stream().anyMatch(packet -> packet.entityId() == labelId), "the first scored jump updates the label");
        }
    }

    @Test
    void endingByTheItemRemovesTheLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            fixture.useItem(scene.run().player());

            assertTrue(scene.labels().isEmpty(), "no label stays behind");
            assertTrue(scene.run().player().getPassengers().isEmpty(), "the runner carries nothing");
        }
    }

    @Test
    void endingByAFallRemovesTheLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            fixture.move(scene.run().player(), new Pos(0.5, 5.0, 0.5), false);

            assertTrue(scene.labels().isEmpty(), "no label stays behind");
        }
    }

    @Test
    void disconnectingRemovesTheLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            env.process().eventHandler().call(new PlayerDisconnectEvent(scene.run().player()));

            assertTrue(scene.labels().isEmpty(), "no label stays behind");
        }
    }

    @Test
    void leavingTheInstanceRemovesTheLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            scene.run().player().setInstance(env.createFlatInstance(), new Pos(0, 40, 0)).join();

            assertTrue(scene.labels().isEmpty(), "no label stays behind in the lobby");
        }
    }

    @Test
    void stoppingTheModuleRemovesTheLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);

            fixture.stopModule();

            assertTrue(scene.labels().isEmpty(), "no label outlives the lobby column");
        }
    }

    @Test
    void aSecondRunGetsItsOwnLabel(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Scene scene = Scene.start(env, fixture);
            fixture.useItem(scene.run().player());

            fixture.useItem(scene.run().player());

            assertEquals(1, scene.labels().size(), "one label for the new run, none left of the old one");
        }
    }
}
