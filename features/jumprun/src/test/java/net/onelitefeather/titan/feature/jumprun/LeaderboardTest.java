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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaderboardTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1ec");
    private static final UUID STEVE = UUID.fromString("00000000-0000-0000-0000-000000005e0e");
    private static final UUID NOTCH = UUID.fromString("00000000-0000-0000-0000-00000000007c");

    private final FakeRunStore store = new FakeRunStore();
    private final Leaderboard leaderboard = new Leaderboard(this.store);
    private CapturedLog log;

    @BeforeEach
    void captureLog() {
        this.log = new CapturedLog(Leaderboard.class);
    }

    @AfterEach
    void releaseLog() {
        this.log.close();
    }

    private static FinishedRun run(UUID player, String name, Mode mode, int score, long secondsAfterBase) {
        return RunStoreContract.run(player, name, mode, score, secondsAfterBase);
    }

    private List<String> namesOf(Mode mode) {
        return this.leaderboard.top(mode).entries().stream().map(TopEntry::name).toList();
    }

    @Test
    void withoutARefreshEveryModeIsEmpty() {
        for (Mode mode : Mode.values()) {
            assertSame(TopThree.EMPTY, this.leaderboard.top(mode), mode + " has no entries yet");
        }
    }

    @Test
    void refreshTakesTheTopThreeOfEveryMode() {
        this.store.append(run(ALEX, "Alex", Mode.HARD, 88, 0));
        this.store.append(run(STEVE, "Steve", Mode.HARD, 61, 1));
        this.store.append(run(NOTCH, "Notch", Mode.EASY, 30, 2));

        this.leaderboard.refresh();

        assertEquals(List.of("Alex", "Steve"), namesOf(Mode.HARD), "hard from the store");
        assertEquals(List.of("Notch"), namesOf(Mode.EASY), "easy from the store");
        assertSame(TopThree.EMPTY, this.leaderboard.top(Mode.ULTRA), "a mode without runs stays empty");
    }

    @Test
    void anOfferedRunSortsInAtOnce() {
        this.store.append(run(ALEX, "Alex", Mode.HARD, 88, 0));
        this.leaderboard.refresh();

        this.leaderboard.offer(run(STEVE, "Steve", Mode.HARD, 90, 5));

        assertEquals(List.of("Steve", "Alex"), namesOf(Mode.HARD), "the new best leads right away");
    }

    @Test
    void anOfferedRunNeverShowsInAnotherMode() {
        this.leaderboard.offer(run(STEVE, "Steve", Mode.HARD, 90, 5));

        assertTrue(this.leaderboard.top(Mode.EASY).entries().isEmpty(), "easy is untouched by a hard run");
        assertEquals(List.of("Steve"), namesOf(Mode.HARD));
    }

    @Test
    void anOfferedRunTheStoreDoesNotHaveYetSurvivesARefresh() {
        this.store.append(run(ALEX, "Alex", Mode.HARD, 88, 0));
        this.leaderboard.offer(run(STEVE, "Steve", Mode.HARD, 90, 5));

        this.leaderboard.refresh();

        assertEquals(List.of("Steve", "Alex"), namesOf(Mode.HARD), "the write to the store is still on its way");
    }

    @Test
    void aRefreshAfterTheStoreCaughtUpKeepsOneEntryPerPlayer() {
        this.leaderboard.offer(run(STEVE, "Steve", Mode.HARD, 90, 5));
        this.store.append(run(STEVE, "Steve", Mode.HARD, 90, 5));

        this.leaderboard.refresh();

        assertEquals(List.of("Steve"), namesOf(Mode.HARD), "a player is listed once");
    }

    @Test
    void aFailedRefreshKeepsTheOldTopAndLogsOneWarning() {
        this.store.append(run(ALEX, "Alex", Mode.HARD, 88, 0));
        this.leaderboard.refresh();
        IllegalStateException failure = new IllegalStateException("database is gone");
        this.store.failWith(failure);

        this.leaderboard.refresh();

        assertEquals(List.of("Alex"), namesOf(Mode.HARD), "the old top stays");
        assertEquals(1, this.log.warnings().size(), "one warning per failed refresh");
        ILoggingEvent line = this.log.warnings().getFirst();
        assertEquals("Could not refresh jump and run leaderboard", line.getMessage());
        assertSame(failure, CapturedLog.causeOf(line), "the exception is logged once");
    }
}
