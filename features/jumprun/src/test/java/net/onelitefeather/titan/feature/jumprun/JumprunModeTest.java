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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Choosing the mode by right-clicking the item while sneaking. */
@ExtendWith(MicrotusExtension.class)
class JumprunModeTest {

    private static Player standingPlayer(Env env, Instance instance) {
        Player player = env.createConnection().connect(instance, StartedRun.STAND);
        player.refreshOnGround(true);
        return player;
    }

    private static void sneakUse(JumprunFixture fixture, Player player) {
        player.setSneaking(true);
        fixture.useItem(player);
        player.setSneaking(false);
    }

    private static String labelText(Instance instance) {
        Entity label = instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.TEXT_DISPLAY).findFirst().orElseThrow();
        return PlainTextComponentSerializer.plainText().serialize(((TextDisplayMeta) label.getEntityMeta()).getText());
    }

    @Test
    void aPlayerStartsInMedium(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Player player = standingPlayer(env, JumprunFixture.loadedInstance(env));

            assertEquals(Mode.MEDIUM, fixture.module().modeOf(player));
        }
    }

    @Test
    void sneakingWithoutARunMovesToTheNextModeAndStartsNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Player player = standingPlayer(env, JumprunFixture.loadedInstance(env));

            sneakUse(fixture, player);

            assertEquals(Mode.HARD, fixture.module().modeOf(player));
            assertFalse(fixture.module().isRunning(player), "choosing a mode does not start a run");
        }
    }

    @Test
    void theFiveModesCycleBackToEasyAndMedium(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Player player = standingPlayer(env, JumprunFixture.loadedInstance(env));

            sneakUse(fixture, player);
            sneakUse(fixture, player);
            assertEquals(Mode.RAINBOW, fixture.module().modeOf(player), "after hard comes rainbow");
            sneakUse(fixture, player);
            assertEquals(Mode.ULTRA, fixture.module().modeOf(player), "after rainbow comes ultra");
            sneakUse(fixture, player);
            assertEquals(Mode.EASY, fixture.module().modeOf(player), "after ultra comes easy");
            sneakUse(fixture, player);

            assertEquals(Mode.MEDIUM, fixture.module().modeOf(player), "after easy comes medium");
        }
    }

    @Test
    void changingTheModeTellsThePlayerAndClicksForThePlayerAlone(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, StartedRun.STAND);
        TestConnection bystanderConnection = env.createConnection();
        bystanderConnection.connect(instance, StartedRun.STAND.add(0, 0, 8));
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
            Collector<EntitySoundEffectPacket> click = connection.trackIncoming(EntitySoundEffectPacket.class);
            Collector<EntitySoundEffectPacket> bystanderHears = bystanderConnection.trackIncoming(EntitySoundEffectPacket.class);

            sneakUse(fixture, player);

            Component expected = fixture.messages().modeChanged(player.getLocale(), Mode.HARD);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
            click.assertSingle(packet -> {
                assertEquals(SoundEvent.UI_BUTTON_CLICK, packet.soundEvent());
                assertEquals(player.getEntityId(), packet.entityId(), "the click follows the player");
            });
            bystanderHears.assertEmpty();
        }
    }

    @Test
    void sneakingInARunChangesNothing(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Collector<SystemChatPacket> chat = run.connection().trackIncoming(SystemChatPacket.class);

            sneakUse(fixture, run.player());

            assertTrue(fixture.module().isRunning(run.player()), "the run is not aborted");
            assertEquals(Mode.MEDIUM, fixture.module().modeOf(run.player()), "the mode stays");
            chat.assertEmpty();
        }
    }

    @Test
    void aNormalUseStartsTheRunInTheChosenMode(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Player player = standingPlayer(env, instance);
            sneakUse(fixture, player);

            fixture.useItem(player);

            assertTrue(fixture.module().isRunning(player), "a run begins");
            assertTrue(labelText(instance).endsWith("Jump & Run · Hard · 0"), "the head label names the mode");
        }
    }

    @Test
    void theHeadLabelNamesRainbowAndUltra(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Player player = standingPlayer(env, instance);
            sneakUse(fixture, player);
            sneakUse(fixture, player);
            fixture.useItem(player);

            assertTrue(labelText(instance).endsWith("Jump & Run · Rainbow · 0"), "rainbow in the head label: " + labelText(instance));
            fixture.useItem(player);
            fixture.settle();
            sneakUse(fixture, player);
            fixture.useItem(player);

            assertTrue(labelText(instance).endsWith("Jump & Run · Ultra · 0"), "ultra in the head label: " + labelText(instance));
        }
    }

    @Test
    void theEndMessageNamesTheModeOfTheRun(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, StartedRun.STAND);
        player.refreshOnGround(true);
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            for (int clicks = 0; clicks < 4; clicks++) {
                sneakUse(fixture, player);
            }
            fixture.useItem(player);
            Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

            fixture.useItem(player);

            Component expected = fixture.messages().endScore(player.getLocale(), Mode.EASY, 0);
            chat.assertSingle(packet -> assertEquals(expected, packet.message()));
        }
    }

    @Test
    void aRunKeepsItsModeWhenTheChoiceChangesAfterItEnds(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            fixture.useItem(run.player());
            sneakUse(fixture, run.player());

            assertEquals(Mode.HARD, fixture.module().modeOf(run.player()), "the choice is made between runs");
            assertFalse(fixture.module().isRunning(run.player()));
        }
    }

    @Test
    void afterADisconnectTheModeIsMediumAgain(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Player player = standingPlayer(env, JumprunFixture.loadedInstance(env));
            sneakUse(fixture, player);

            env.process().eventHandler().call(new PlayerDisconnectEvent(player));

            assertEquals(Mode.MEDIUM, fixture.module().modeOf(player));
        }
    }
}
