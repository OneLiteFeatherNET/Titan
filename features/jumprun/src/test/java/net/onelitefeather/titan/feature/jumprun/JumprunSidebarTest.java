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

import static net.onelitefeather.titan.feature.jumprun.SidebarPackets.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.network.packet.server.play.UpdateScorePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The sidebar of the runner: score and record during a run, and gone when it ends. */
@ExtendWith(MicrotusExtension.class)
class JumprunSidebarTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1ec");
    private static final int BEST = 42;

    private static SidebarScene startHard(Env env, JumprunFixture fixture, Instance instance) {
        return SidebarScene.start(env, fixture, instance, ALEX, "Alex", Mode.HARD, player -> fixture.records().submit(JumprunFixture.finished(ALEX, Mode.HARD, BEST)));
    }

    private static void call(Env env, Event event) {
        env.process().eventHandler().call(event);
    }

    @Test
    void theRunnerGetsAnObjectiveWithTheTitleOfTheMode(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            List<ServerPacket> sent = scene.collect();

            assertEquals(1, SidebarPackets.created(sent).size(), "one objective");
            assertEquals("[block/slime_block] Jump & Run · Hard", plain(SidebarPackets.created(sent).getFirst().objectiveValue()), "the title is the slime sprite, the game and the mode");
            assertEquals(1, SidebarPackets.displays(sent).size(), "shown in the sidebar slot");
        }
    }

    @Test
    void theRunnerSeesScoreZeroAndTheStoredRecord(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            assertEquals(List.of("Score|0", "Record|" + BEST), SidebarPackets.shownRows(scene.collect()), "score and record, and no top lines without a leaderboard");
        }
    }

    @Test
    void scoreAndRecordShowTheirValueInTheNumberColumn(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            List<UpdateScorePacket> scores = SidebarPackets.scores(scene.collect());

            assertEquals(2, scores.size(), "one score entry per line");
            assertEquals(List.of("0", "" + BEST), scores.stream().map(score -> plain(score.numberFormat().content())).toList(), "the values stand in the number column");
        }
    }

    @Test
    void aSecondPlayerGetsNoSidebar(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Instance instance = JumprunFixture.loadedInstance(env);
            TestConnection bystander = env.createConnection();
            bystander.connect(instance, StartedRun.STAND.add(0, 0, 8));
            Collector<ServerPacket> bystanderSent = bystander.trackIncoming();

            SidebarScene scene = startHard(env, fixture, instance);
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 1);

            assertTrue(SidebarPackets.sidebarPackets(bystanderSent.collect()).isEmpty(), "the sidebar is the runner's alone");
        }
    }

    @Test
    void aScoreOfSevenUpdatesOnlyTheScoreValueAndTheActionBarShowsSeven(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 6);
            Collector<ServerPacket> sent = scene.run().connection().trackIncoming();

            List<ServerPacket> packets = scene.run().landOnNext();
            List<ServerPacket> tracked = sent.collect();

            assertEquals(List.of(), SidebarPackets.written(tracked), "the label is not sent again");
            assertEquals(List.of("7"), SidebarPackets.scores(tracked).stream().map(score -> plain(score.numberFormat().content())).toList(), "only the changed value is sent");
            ActionBarPacket bar = packets.stream().filter(ActionBarPacket.class::isInstance).map(ActionBarPacket.class::cast).findFirst().orElseThrow();
            assertEquals(plain(fixture.messages().scoreActionBar(scene.player().getLocale(), 7)), plain(bar.text()), "the action bar shows 7 as before");
        }
    }

    @Test
    void theRecordLineFollowsTheScoreOncePastTheRecord(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), ALEX, "Alex", Mode.HARD, player -> fixture.records().submit(JumprunFixture.finished(ALEX, Mode.HARD, 2)));
            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 3);

            assertEquals(List.of("Score|3", "Record|3"), SidebarPackets.shownRows(scene.collect()), "the record is never below the score");
        }
    }

    @Test
    void abortingRemovesTheObjective(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            fixture.useItem(scene.player());

            assertEquals(1, SidebarPackets.removed(scene.collect()).size(), "the sidebar goes with the run");
        }
    }

    @Test
    void aFallRemovesTheObjective(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            fixture.move(scene.player(), new Pos(0.5, 5.0, 0.5), false);

            assertEquals(1, SidebarPackets.removed(scene.collect()).size(), "the sidebar goes with the run");
        }
    }

    @Test
    void dyingRemovesTheObjective(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            call(env, new PlayerDeathEvent(scene.player(), Component.empty(), Component.empty()));

            assertEquals(1, SidebarPackets.removed(scene.collect()).size(), "the sidebar goes with the run");
        }
    }

    @Test
    void disconnectingRemovesTheObjective(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            call(env, new PlayerDisconnectEvent(scene.player()));

            assertEquals(1, SidebarPackets.removed(scene.collect()).size(), "the sidebar goes with the run");
        }
    }

    @Test
    void leavingTheInstanceRemovesTheObjective(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            scene.player().setInstance(env.createFlatInstance(), new Pos(0, 40, 0)).join();

            assertEquals(1, SidebarPackets.removed(scene.collect()).size(), "the sidebar goes with the run");
        }
    }

    @Test
    void stoppingTheModuleRemovesTheObjective(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));

            fixture.stopModule();

            assertEquals(1, SidebarPackets.removed(scene.collect()).size(), "the sidebar does not outlive the lobby column");
        }
    }

    @Test
    void aNewRunGetsAFreshSidebar(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = startHard(env, fixture, JumprunFixture.loadedInstance(env));
            fixture.useItem(scene.player());

            fixture.useItem(scene.player());

            List<ServerPacket> sent = scene.collect();
            assertEquals(2, SidebarPackets.created(sent).size(), "one objective per run");
            assertEquals(1, SidebarPackets.removed(sent).size(), "the first one is gone, the second is running");
            assertFalse(SidebarPackets.created(sent).get(0).objectiveName().equals(SidebarPackets.created(sent).get(1).objectiveName()), "each run has its own objective");
        }
    }
}
