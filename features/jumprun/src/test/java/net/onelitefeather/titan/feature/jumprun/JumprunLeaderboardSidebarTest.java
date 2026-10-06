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

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.EndReason;
import net.onelitefeather.titan.feature.jumprun.persistence.FakeRunStore;
import net.onelitefeather.titan.feature.jumprun.persistence.FinishedRun;
import net.onelitefeather.titan.feature.jumprun.persistence.InMemoryRunRecords;
import net.onelitefeather.titan.feature.jumprun.persistence.RunStore;
import net.onelitefeather.titan.feature.jumprun.persistence.TopThree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The top three in the sidebar of a Hard run. The scores are small, so a run reaches them in a
 * few jumps; what matters is the order and the highlight, not the size of the numbers.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunLeaderboardSidebarTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1ec");
    private static final UUID STEVE = UUID.fromString("00000000-0000-0000-0000-000000005e7e");
    private static final UUID NOTCH = UUID.fromString("00000000-0000-0000-0000-00000000007c");
    private static final UUID JEB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final int REFRESH_TICKS = 30 * ServerFlag.SERVER_TICKS_PER_SECOND;

    private final FakeRunStore store = new FakeRunStore();
    private final Queue<Runnable> pending = new ArrayDeque<>();
    private final Executor deferred = pending::add;

    private void stored(UUID player, String name, Mode mode, int score) {
        store.append(new FinishedRun(player, name, mode, score, EndReason.FALL, Instant.parse("2026-10-03T11:00:00Z").plusSeconds(score)));
    }

    private void storedTopThree() {
        stored(ALEX, "Alex", Mode.HARD, 8);
        stored(STEVE, "Steve", Mode.HARD, 6);
        stored(NOTCH, "Notch", Mode.HARD, 4);
    }

    /**
     * A fixture whose refreshes run at once and that has refreshed already, so the board is filled.
     */
    private JumprunFixture fixtureWithBoard(Env env, InMemoryRunRecords records, Leaderboard board) {
        JumprunFixture fixture = JumprunFixture.start(env, records, board, Runnable::run);
        fixture.module().refreshLeaderboard();
        return fixture;
    }

    private static String other(String name, int score) {
        return " " + SidebarPackets.HEAD + " " + name + "|" + score;
    }

    private static String own(String name, int score) {
        return "»" + SidebarPackets.HEAD + " " + name + "|" + score;
    }

    @Test
    void theRunnerSeesTheTopThreeWithTheOwnLineMarked(Env env) {
        storedTopThree();
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = fixtureWithBoard(env, records, new Leaderboard(store))) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), STEVE, "Steve", Mode.HARD, player -> records.submit(JumprunFixture.finished(STEVE, Mode.HARD, 6)));

            List<String> rows = SidebarPackets.shownRows(scene.collect());

            assertEquals(List.of("Score|0", "Record|6", "", "Top 3", other("Alex", 8), own("Steve", 6), other("Notch", 4)), rows, "score, record, a gap, the header and the top three with the own line marked");
        }
    }

    @Test
    void passingTheThirdPlaceMidRunShowsTheRunnerAtOnceAndDropsTheOneBelow(Env env) {
        storedTopThree();
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = fixtureWithBoard(env, records, new Leaderboard(store))) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), JEB, "Jeb", Mode.HARD, player -> {
            });

            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 5);

            List<String> rows = SidebarPackets.shownRows(scene.collect());
            assertEquals(List.of("Score|5", "Record|5", "", "Top 3", other("Alex", 8), other("Steve", 6), own("Jeb", 5)), rows, "Jeb moves in and Notch is out, while the run is still going");
        }
    }

    @Test
    void aRecordInAnotherModeDoesNotShowInHard(Env env) {
        storedTopThree();
        stored(UUID.randomUUID(), "Zed", Mode.EASY, 99);
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = fixtureWithBoard(env, records, new Leaderboard(store))) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), JEB, "Jeb", Mode.HARD, player -> records.submit(JumprunFixture.finished(JEB, Mode.EASY, 30)));

            List<String> lines = SidebarPackets.shownRows(scene.collect());

            assertEquals("Record|–", lines.get(1), "the Easy record of 30 is not a Hard record");
            assertFalse(lines.stream().anyMatch(line -> line.contains("Zed")), "no entry of another mode");
        }
    }

    @Test
    void aRefreshWithNewStoreDataUpdatesTheRunningSidebar(Env env) {
        storedTopThree();
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = fixtureWithBoard(env, records, new Leaderboard(store))) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), STEVE, "Steve", Mode.HARD, player -> records.submit(JumprunFixture.finished(STEVE, Mode.HARD, 6)));
            stored(JEB, "Jeb", Mode.HARD, 7);

            fixture.module().refreshLeaderboard();

            List<String> rows = SidebarPackets.shownRows(scene.collect());
            assertEquals(List.of(other("Alex", 8), other("Jeb", 7), own("Steve", 6)), rows.subList(4, 7), "Jeb is new in second place and Steve's own line moved one place lower");
        }
    }

    @Test
    void theScheduledTaskOnlyHandsTheRefreshOverAndRunsEveryThirtySeconds(Env env) {
        storedTopThree();
        Leaderboard board = new Leaderboard(store);
        try (JumprunFixture fixture = JumprunFixture.start(env, new InMemoryRunRecords(), board, deferred)) {
            assertTrue(pending.isEmpty(), "starting reads nothing, the database writer is not wired yet");
            env.tick();
            assertEquals(1, pending.size(), "the first refresh is handed over on the next tick");
            pending.poll().run();
            stored(JEB, "Jeb", Mode.HARD, 7);

            for (int tick = 0; tick < REFRESH_TICKS - 1; tick++) {
                env.tick();
            }
            assertTrue(pending.isEmpty(), "nothing before the thirty seconds are over");
            env.tick();

            assertEquals(1, pending.size(), "the task handed one refresh over");
            assertEquals("Alex", board.top(Mode.HARD).entries().getFirst().name(), "the task itself did not read the store");
            assertEquals(3, board.top(Mode.HARD).entries().size(), "the board has not changed yet");
            pending.poll().run();
            assertEquals("Jeb", board.top(Mode.HARD).entries().get(1).name(), "the executor's refresh read the store");
        }
    }

    @Test
    void stoppingTheModuleCancelsTheTask(Env env) {
        Leaderboard board = new Leaderboard(store);
        try (JumprunFixture fixture = JumprunFixture.start(env, new InMemoryRunRecords(), board, deferred)) {
            fixture.stopModule();
            for (int tick = 0; tick < REFRESH_TICKS; tick++) {
                env.tick();
            }

            assertTrue(pending.isEmpty(), "no refresh after the stop");
        }
    }

    @Test
    void withoutALeaderboardNothingIsScheduledAndThereAreNoTopLines(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), JEB, "Jeb", Mode.HARD, player -> {
            });

            fixture.module().refreshLeaderboard();
            for (int tick = 0; tick < REFRESH_TICKS; tick++) {
                env.tick();
            }

            List<ServerPacket> sent = scene.collect();
            assertEquals(List.of("Score|0", "Record|–"), SidebarPackets.shownRows(sent), "no top lines");
        }
    }

    @Test
    void anEmptyBoardShowsNothingBelowTheRecord(Env env) {
        try (JumprunFixture fixture = fixtureWithBoard(env, new InMemoryRunRecords(), new Leaderboard(store))) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), JEB, "Jeb", Mode.HARD, player -> {
            });

            assertEquals(List.of("Score|0", "Record|–"), SidebarPackets.shownRows(scene.collect()), "an empty board shows no gap, header or places");
        }
    }

    /** Stores a best of 3 for Jeb next to the usual top three, which it does not reach. */
    private SidebarScene jebWithBestOfThree(Env env, JumprunFixture fixture, InMemoryRunRecords records) {
        return SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), JEB, "Jeb", Mode.HARD, player -> records.submit(JumprunFixture.finished(JEB, Mode.HARD, 3)));
    }

    @Test
    void aRunnerWhoPassesTheOwnBestAndTheThirdPlaceIsOfferedAndMarked(Env env) {
        storedTopThree();
        stored(JEB, "Jeb", Mode.HARD, 3);
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = fixtureWithBoard(env, records, new Leaderboard(store))) {
            SidebarScene scene = jebWithBestOfThree(env, fixture, records);

            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 5);

            List<String> rows = SidebarPackets.shownRows(scene.collect());
            assertEquals(List.of("Score|5", "Record|5", "", "Top 3", other("Alex", 8), other("Steve", 6), own("Jeb", 5)), rows, "5 beats the best of 3 and the third place");
        }
    }

    @Test
    void aRunnerWhoStaysBelowTheOwnBestIsNotOffered(Env env) {
        storedTopThree();
        stored(JEB, "Jeb", Mode.HARD, 3);
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = fixtureWithBoard(env, records, new Leaderboard(store))) {
            SidebarScene scene = jebWithBestOfThree(env, fixture, records);

            scene.run().landOnNext(JumprunFixture.ASCENT_JUMPS + 2);

            List<String> lines = SidebarPackets.shownRows(scene.collect());
            assertEquals(List.of("Score|2", "Record|3", "", "Top 3", other("Alex", 8), other("Steve", 6), other("Notch", 4)), lines, "2 is below the best of 3, so Jeb is not on the board");
        }
    }

    @Test
    void aRefreshAfterTheRunEndedDoesNotCreateTheSidebarAgain(Env env) {
        storedTopThree();
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = fixtureWithBoard(env, records, new Leaderboard(store))) {
            SidebarScene scene = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), JEB, "Jeb", Mode.HARD, player -> {
            });
            fixture.useItem(scene.player());
            scene.collect();
            var afterEnd = scene.run().connection().trackIncoming();

            fixture.module().refreshLeaderboard();

            assertEquals(List.of(), SidebarPackets.sidebarPackets(afterEnd.collect()), "no sidebar packet for a run that is over");
        }
    }

    /** A leaderboard that fails to read the top of one mode, once armed. */
    private static final class FailingBoard extends Leaderboard {

        private final AtomicBoolean armed = new AtomicBoolean();

        FailingBoard(RunStore store) {
            super(store);
        }

        @Override
        TopThree top(Mode mode) {
            if (armed.getAndSet(false)) {
                throw new IllegalStateException("no top for " + mode);
            }
            return super.top(mode);
        }
    }

    @Test
    void aFailingSidebarUpdateDoesNotStopTheOtherRunsAndIsLogged(Env env) {
        storedTopThree();
        FailingBoard board = new FailingBoard(store);
        InMemoryRunRecords records = new InMemoryRunRecords();
        try (JumprunFixture fixture = JumprunFixture.start(env, records, board, Runnable::run); CapturedLog log = new CapturedLog(JumprunModule.class)) {
            SidebarScene first = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), ALEX, "Alex", Mode.HARD, player -> {
            });
            SidebarScene second = SidebarScene.start(env, fixture, JumprunFixture.loadedInstance(env), STEVE, "Steve", Mode.MEDIUM, player -> {
            });
            // After the start, whose ticks already ran the scheduled refresh.
            stored(JEB, "Jeb", Mode.HARD, 7);
            stored(JEB, "Jeb", Mode.MEDIUM, 7);
            board.armed.set(true);

            fixture.module().refreshLeaderboard();

            List<Boolean> updated = List.of(first, second).stream().map(scene -> SidebarPackets.shownRows(scene.collect()).stream().anyMatch(line -> line.contains("Jeb"))).toList();
            assertEquals(1, updated.stream().filter(shown -> shown).count(), "exactly one run got the new board, the other one failed");
            assertEquals(1, log.warnings().size(), "one WARN for the one failure");
            var warning = log.warnings().getFirst();
            UUID failed = updated.getFirst() ? STEVE : ALEX;
            assertEquals(failed, CapturedLog.valueOf(warning, "player"), "the WARN names the run that failed");
            assertEquals("IllegalStateException", CapturedLog.causeOf(warning).getClass().getSimpleName(), "the WARN carries the cause");
        }
    }
}
