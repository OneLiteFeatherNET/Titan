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
package net.onelitefeather.titan.feature.portal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.SourceType;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Drives the label display through a real {@code Env}; time passes only through {@code env.tick()}.
 */
@ExtendWith(MicrotusExtension.class)
class PortalLabelDisplayTest {

    private static final int PERIOD_TICKS = 20;

    private final FakePlayerCounts counts = new FakePlayerCounts();

    private static PortalLabel label(String text, @Nullable String offline, @Nullable LabelSource source, Billboard billboard, float yaw) {
        return new PortalLabel(new Vec(12.5, 66.0, -3.5), text, offline, source, billboard, yaw);
    }

    private static Portal portal(String id, @Nullable PortalLabel label) {
        return new Portal(id, new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Survival", null, label);
    }

    private PortalModule start(Env env, TestTitanNode titan, Instance lobby, Executor executor, Portal... portals) {
        return start(env, titan, lobby, executor, this.counts, portals);
    }

    private PortalModule start(Env env, TestTitanNode titan, Instance lobby, Executor executor, PlayerCounts provider, Portal... portals) {
        PortalModule module = new PortalModule(titan.node(), () -> List.of(portals), new RecordingDeliver(), new FakePermissionService(), new AdjustableClock(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC), lobby, env.process().scheduler(), executor, new LabelReader(provider, () -> 9), new PortalSettings(1));
        module.start();
        return module;
    }

    private static List<Entity> displays(Instance lobby) {
        return lobby.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.TEXT_DISPLAY).toList();
    }

    private static String text(Entity display) {
        return PlainTextComponentSerializer.plainText().serialize(((TextDisplayMeta) display.getEntityMeta()).getText());
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    @DisplayName("Each portal with a label gets exactly one display, a portal without one gets none")
    @Test
    void oneDisplayPerLabel(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = start(env, titan, lobby, Runnable::run, portal("a", label("a", null, null, Billboard.CENTER, 0f)), portal("b", label("b", null, null, Billboard.CENTER, 0f)), portal("plain", null));

            Assertions.assertEquals(2, displays(lobby).size(), "two labels, two displays, nothing for the portal without a label");
            module.stop();
        }
    }

    @DisplayName("A portal without a label starts as before: no display, nothing scheduled")
    @Test
    void noLabelNoDisplay(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            QueuedExecutor executor = new QueuedExecutor();
            PortalModule module = start(env, titan, lobby, executor, portal("plain", null));

            tick(env, 3 * PERIOD_TICKS);

            Assertions.assertTrue(displays(lobby).isEmpty(), "no label, no display");
            Assertions.assertEquals(0, executor.pending(), "no label, no read");
            module.stop();
        }
    }

    @DisplayName("The display stands at the label position and turns to the player by default")
    @Test
    void positionAndDefaultBillboard(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = start(env, titan, lobby, Runnable::run, portal("a", label("a", null, null, Billboard.CENTER, 90f)));

            Entity display = displays(lobby).get(0);

            Assertions.assertEquals(new Pos(12.5, 66.0, -3.5, 0f, 0f), display.getPosition(), "the position of the label, the yaw only counts for fixed");
            Assertions.assertEquals(BillboardConstraints.CENTER, ((TextDisplayMeta) display.getEntityMeta()).getBillboardRenderConstraints());
            module.stop();
        }
    }

    @DisplayName("A fixed billboard keeps the yaw of the label")
    @Test
    void fixedBillboardKeepsYaw(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = start(env, titan, lobby, Runnable::run, portal("a", label("a", null, null, Billboard.FIXED, 90f)));

            Entity display = displays(lobby).get(0);

            Assertions.assertEquals(BillboardConstraints.FIXED, ((TextDisplayMeta) display.getEntityMeta()).getBillboardRenderConstraints());
            Assertions.assertEquals(90f, display.getPosition().yaw(), "the yaw of the label");
            module.stop();
        }
    }

    @DisplayName("A provider that is not CloudNet shows 3/20 for its task")
    @Test
    void anyProviderShowsItsCount(Env env) {
        this.counts.set(SourceType.TASK, "Survival", new PlayerCount(3, 20, true));
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = start(env, titan, lobby, Runnable::run, portal("a", label("<online>/<max>", null, null, Billboard.CENTER, 0f)));

            tick(env, 2);

            Assertions.assertEquals("3/20", text(displays(lobby).get(0)));
            module.stop();
        }
    }

    @DisplayName("The local source shows the local number with '/?'")
    @Test
    void localSource(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = start(env, titan, lobby, Runnable::run, portal("a", label("<online>/<max>", null, new LabelSource.Local(), Billboard.CENTER, 0f)));

            tick(env, 2);

            Assertions.assertEquals("9/?", text(displays(lobby).get(0)));
            module.stop();
        }
    }

    @DisplayName("The text follows the counts after the configured period, from offline to online too")
    @Test
    void textFollowsTheCountsAfterThePeriod(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = start(env, titan, lobby, Runnable::run, portal("a", label("<online>/<max>", "offline", null, Billboard.CENTER, 0f)));
            tick(env, 2);
            Assertions.assertEquals("offline", text(displays(lobby).get(0)), "nothing runs at the start");

            this.counts.set(SourceType.TASK, "Survival", new PlayerCount(1, 20, true));
            tick(env, PERIOD_TICKS - 2);
            Assertions.assertEquals("offline", text(displays(lobby).get(0)), "the next read is a full period after the first");

            tick(env, 3);
            Assertions.assertEquals("1/20", text(displays(lobby).get(0)), "the server came up");
            module.stop();
        }
    }

    @DisplayName("A read that is still running makes the next period skip instead of piling up")
    @Test
    void slowReadSkipsThePeriod(Env env) {
        this.counts.set(SourceType.TASK, "Survival", new PlayerCount(1, 20, true));
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            QueuedExecutor executor = new QueuedExecutor();
            PortalModule module = start(env, titan, lobby, executor, portal("a", label("<online>/<max>", null, null, Billboard.CENTER, 0f)));

            tick(env, 3 * PERIOD_TICKS);
            Assertions.assertEquals(1, executor.pending(), "three periods passed while the first read is still running: only one read was started");
            Assertions.assertEquals(0, this.counts.reads(), "the read has not run yet");
            Assertions.assertEquals("", text(displays(lobby).get(0)), "nothing is applied before the read is done");

            executor.runAll();
            Assertions.assertEquals("", text(displays(lobby).get(0)), "the read ran off the tick; the text is applied by the scheduler, not by the reader");
            tick(env, 2);
            Assertions.assertEquals("1/20", text(displays(lobby).get(0)), "the scheduler applies the snapshot");

            tick(env, PERIOD_TICKS);
            Assertions.assertEquals(1, executor.pending(), "once the read is done the next period starts a new one");
            module.stop();
        }
    }

    @DisplayName("Stopping removes the displays and ends the refresh")
    @Test
    void stopRemovesTheDisplays(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            QueuedExecutor executor = new QueuedExecutor();
            PortalModule module = start(env, titan, lobby, executor, portal("a", label("a", null, null, Billboard.CENTER, 0f)));
            tick(env, 2);

            module.stop();
            executor.runAll();
            tick(env, 3 * PERIOD_TICKS);

            Assertions.assertTrue(displays(lobby).isEmpty(), "all displays are gone");
            Assertions.assertEquals(0, executor.pending(), "no read after stop");
        }
    }

    @DisplayName("A refresh that finds the same numbers sends no second metadata update")
    @Test
    void unchangedNumbersSendNothing(Env env) {
        this.counts.set(SourceType.TASK, "Survival", new PlayerCount(3, 20, true));
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            connection.connect(lobby);
            Collector<EntityMetaDataPacket> updates = connection.trackIncoming(EntityMetaDataPacket.class);
            PortalModule module = start(env, titan, lobby, Runnable::run, portal("a", label("<online>/<max>", null, null, Billboard.CENTER, 0f)));
            tick(env, 2);
            Assertions.assertEquals("3/20", text(displays(lobby).get(0)), "the first read is shown");
            int displayId = displays(lobby).get(0).getEntityId();
            long before = updates.collect().stream().filter(packet -> packet.entityId() == displayId).count();
            Assertions.assertTrue(before > 0, "the viewer saw the display and its first text");

            tick(env, 3 * PERIOD_TICKS);

            long after = updates.collect().stream().filter(packet -> packet.entityId() == displayId).count();
            Assertions.assertEquals(before, after, "the same numbers must not be sent again");
            Assertions.assertTrue(this.counts.reads() >= 3, "the provider was read in every period, only the packet was spared");
            module.stop();
        }
    }

    @DisplayName("Without a provider the offline text is shown end to end")
    @Test
    void offlineTextWithoutProvider(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = start(env, titan, lobby, Runnable::run, new NoPlayerCounts(), portal("a", label("<online>/<max>", "<red>Offline", null, Billboard.CENTER, 0f)));

            tick(env, 2);

            Assertions.assertEquals("Offline", text(displays(lobby).get(0)), "the fallback provider reads nothing as running");
            module.stop();
        }
    }

    @DisplayName("A start that fails after spawning removes the displays again")
    @Test
    void failedStartCleansUp(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Instance lobby = env.createFlatInstance();
            PortalModule module = new PortalModule(titan.node(), () -> List.of(portal("a", label("a", null, null, Billboard.CENTER, 0f))), new RecordingDeliver(), new FakePermissionService(), new AdjustableClock(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC), lobby, env.process().scheduler(), Runnable::run, new LabelReader(this.counts, () -> 9), new PortalSettings(Integer.MAX_VALUE));

            Assertions.assertThrows(ArithmeticException.class, module::start, "the period overflows");

            Assertions.assertTrue(displays(lobby).isEmpty(), "no display is left behind");
        }
    }
}
