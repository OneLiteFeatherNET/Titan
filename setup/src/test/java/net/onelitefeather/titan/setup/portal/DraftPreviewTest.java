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
package net.onelitefeather.titan.setup.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class DraftPreviewTest {

    private PortalEditor editor;
    private DraftPreview preview;
    private Player player;
    private TestConnection connection;
    private Collector<ParticlePacket> particles;
    private Collector<SystemChatPacket> chat;
    private Collector<ParticlePacket> bystander;

    @BeforeEach
    void setUp(Env env) {
        editor = new PortalEditor(new InMemoryPortalStore());
        preview = new DraftPreview(editor);
        Instance instance = env.createFlatInstance();
        connection = env.createConnection();
        player = connection.connect(instance, new Pos(0.5, 40, 0.5));
        particles = connection.trackIncoming(ParticlePacket.class);
        chat = connection.trackIncoming(SystemChatPacket.class);
        TestConnection other = env.createConnection();
        other.connect(instance, new Pos(0.5, 40, 0.5));
        bystander = other.trackIncoming(ParticlePacket.class);
    }

    @DisplayName("Particles go to the draft's player only, at most the cap per run")
    @Test
    void particlesGoToTheDraftPlayerOnly(Env env) {
        editor.corner1(player.getUuid(), "p", player.getPosition());
        preview.start(player, "p");

        tick(env, 20);

        List<ParticlePacket> shown = particles.collect();
        assertFalse(shown.isEmpty(), "the draft is shown");
        assertTrue(bystander.collect().isEmpty(), "nobody else sees it");
        assertTrue(shown.size() <= 5 * DraftOutline.MAX_POINTS, "at most MAX_POINTS per run over 4-5 runs");
    }

    @DisplayName("A box with one corner follows the player as they move")
    @Test
    void boxFollowsThePlayer(Env env) {
        editor.corner1(player.getUuid(), "p", player.getPosition());
        preview.start(player, "p");
        tick(env, 6);
        List<ParticlePacket> near = particles.collect();

        player.teleport(new Pos(20.5, 40, 0.5));
        env.tick();
        particles = connection.trackIncoming(ParticlePacket.class);
        tick(env, 6);
        List<ParticlePacket> after = particles.collect();

        assertTrue(after.stream().anyMatch(packet -> packet.x() > 20), "a corner of the box now reaches the new block");
        assertTrue(near.stream().noneMatch(packet -> packet.x() > 20), "before moving the box stayed near the first corner");
    }

    @DisplayName("Saving ends the preview and leaves no task behind")
    @Test
    void savingStopsThePreview(Env env) {
        complete("p");
        preview.start(player, "p");
        tick(env, 6);

        editor.save(player.getUuid(), "p");
        preview.stop(player.getUuid(), "p");
        particles = connection.trackIncoming(ParticlePacket.class);
        tick(env, 30);

        assertTrue(particles.collect().isEmpty(), "no particles after save");
        assertEquals(0, preview.running(), "no task left");
    }

    @DisplayName("Cancelling ends the preview")
    @Test
    void cancelStopsThePreview(Env env) {
        complete("p");
        preview.start(player, "p");
        tick(env, 6);

        editor.cancel(player.getUuid(), "p");
        preview.stop(player.getUuid(), "p");
        particles = connection.trackIncoming(ParticlePacket.class);
        tick(env, 30);

        assertTrue(particles.collect().isEmpty(), "no particles after cancel");
        assertEquals(0, preview.running(), "no task left");
    }

    @DisplayName("A draft that vanished ends its own task")
    @Test
    void vanishedDraftEndsTheTask(Env env) {
        complete("p");
        preview.start(player, "p");
        tick(env, 6);

        editor.discardAll(player.getUuid());
        tick(env, 10);

        assertEquals(0, preview.running(), "the task noticed the missing draft");
    }

    @DisplayName("Stopping one draft leaves the preview of another draft of the player running")
    @Test
    void stoppingAnotherDraftKeepsThePreview(Env env) {
        complete("p");
        complete("q");
        preview.start(player, "q");

        preview.stop(player.getUuid(), "p");

        assertEquals(1, preview.running(), "q is still previewed");
    }

    @DisplayName("Disconnect stops the preview")
    @Test
    void stopByPlayerEndsThePreview(Env env) {
        complete("p");
        preview.start(player, "p");
        tick(env, 6);

        preview.stop(player.getUuid());

        assertEquals(0, preview.running(), "no task left");
    }

    @DisplayName("Starting another draft replaces the task instead of adding one")
    @Test
    void startingAnotherDraftReplacesTheTask(Env env) {
        complete("p");
        complete("q");
        preview.start(player, "p");
        preview.start(player, "q");

        assertEquals(1, preview.running(), "one task per player");
        preview.stop(player.getUuid(), "p");
        assertEquals(1, preview.running(), "the task now belongs to q");
    }

    @DisplayName("The default-radius hint is sent once, not on every run")
    @Test
    void defaultRadiusHintComesOnce(Env env) {
        editor.shape(player.getUuid(), "r", PortalDraft.Form.RING);
        preview.start(player, "r");

        tick(env, 40);

        assertEquals(1, chat.collect().size(), "exactly one hint over eight runs");
    }

    @DisplayName("Starting the same draft again keeps the task and does not repeat the hint")
    @Test
    void restartOfTheSameDraftKeepsTheHintQuiet(Env env) {
        editor.shape(player.getUuid(), "r", PortalDraft.Form.RING);
        preview.start(player, "r");
        tick(env, 6);

        preview.start(player, "r");
        tick(env, 30);

        assertEquals(1, preview.running(), "still one task");
        assertEquals(1, chat.collect().size(), "the hint came only once");
    }

    @DisplayName("A box draft never gets the radius hint")
    @Test
    void boxGetsNoHint(Env env) {
        editor.corner1(player.getUuid(), "p", player.getPosition());
        preview.start(player, "p");

        tick(env, 20);

        assertTrue(chat.collect().isEmpty(), "no hint for a box");
    }

    private void complete(String id) {
        editor.corner1(player.getUuid(), id, new Pos(0, 40, 0));
        editor.corner2(player.getUuid(), id, new Pos(2, 42, 2));
        editor.task(player.getUuid(), id, "Task");
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }
}
