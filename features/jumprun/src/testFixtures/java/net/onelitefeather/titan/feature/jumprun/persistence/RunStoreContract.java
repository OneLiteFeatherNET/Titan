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
package net.onelitefeather.titan.feature.jumprun.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import org.junit.jupiter.api.Test;

/**
 * What every {@link RunStore} must do. The Postgres store and the in-memory fake run the same
 * cases, so the fake the unit tests lean on cannot drift from the real one.
 */
public interface RunStoreContract {

    Instant BASE = Instant.parse("2026-01-01T00:00:00Z");
    UUID ALEX = new UUID(0, 1);
    UUID STEVE = new UUID(0, 2);
    UUID NOTCH = new UUID(0, 3);
    UUID JEB = new UUID(0, 4);

    /**
     * The store under test; the same instance on every call within one test, empty at the start.
     */
    RunStore store();

    static FinishedRun run(UUID player, String name, Mode mode, int score, long secondsAfterBase) {
        return new FinishedRun(player, name, mode, score, EndReason.FALL, BASE.plusSeconds(secondsAfterBase));
    }

    private List<String> hardNames() {
        return store().topThreeOfEveryMode().get(Mode.HARD).entries().stream().map(TopEntry::name).toList();
    }

    @Test
    default void appendedRunWithScoreZero_isStored() {
        store().append(run(ALEX, "Alex", Mode.HARD, 0, 0));

        assertEquals(Map.of(Mode.HARD, 0), store().bestsOf(ALEX), "a run that scored nothing still counts");
    }

    @Test
    default void bestsOf_isTheMaximumPerMode() {
        store().append(run(ALEX, "Alex", Mode.HARD, 10, 0));
        store().append(run(ALEX, "Alex", Mode.HARD, 25, 1));
        store().append(run(ALEX, "Alex", Mode.HARD, 15, 2));
        store().append(run(ALEX, "Alex", Mode.EASY, 7, 3));

        assertEquals(Map.of(Mode.HARD, 25, Mode.EASY, 7), store().bestsOf(ALEX), "the best of each mode, not the latest");
    }

    @Test
    default void bestsOf_ignoresOtherPlayers() {
        store().append(run(ALEX, "Alex", Mode.HARD, 10, 0));
        store().append(run(STEVE, "Steve", Mode.HARD, 99, 1));

        assertEquals(Map.of(Mode.HARD, 10), store().bestsOf(ALEX), "Steve's 99 is not Alex's");
    }

    @Test
    default void bestsOf_aPlayerWithoutRuns_isEmpty() {
        store().append(run(STEVE, "Steve", Mode.HARD, 99, 0));

        assertTrue(store().bestsOf(ALEX).isEmpty(), "no runs, no bests");
    }

    @Test
    default void topThree_ofAMode_isTheThreeBestPlayersInOrder() {
        store().append(run(JEB, "Jeb", Mode.HARD, 30, 0));
        store().append(run(NOTCH, "Notch", Mode.HARD, 42, 1));
        store().append(run(ALEX, "Alex", Mode.HARD, 88, 2));
        store().append(run(STEVE, "Steve", Mode.HARD, 61, 3));

        assertEquals(List.of(new TopEntry(ALEX, "Alex", 88, BASE.plusSeconds(2)), new TopEntry(STEVE, "Steve", 61, BASE.plusSeconds(3)), new TopEntry(NOTCH, "Notch", 42, BASE.plusSeconds(1))), store().topThreeOfEveryMode().get(Mode.HARD).entries(), "Jeb is fourth and missing");
    }

    @Test
    default void topThree_listsAPlayerOnceWithTheBestRun() {
        store().append(run(ALEX, "Alex", Mode.HARD, 20, 0));
        store().append(run(ALEX, "Alex", Mode.HARD, 50, 1));
        store().append(run(ALEX, "Alex", Mode.HARD, 30, 2));
        store().append(run(STEVE, "Steve", Mode.HARD, 40, 3));

        assertEquals(List.of(new TopEntry(ALEX, "Alex", 50, BASE.plusSeconds(1)), new TopEntry(STEVE, "Steve", 40, BASE.plusSeconds(3))), store().topThreeOfEveryMode().get(Mode.HARD).entries(), "Alex only once, with 50");
    }

    @Test
    default void topThree_onATie_theEarlierRunWins() {
        store().append(run(ALEX, "Alex", Mode.HARD, 60, 20));
        store().append(run(STEVE, "Steve", Mode.HARD, 60, 10));

        assertEquals(List.of("Steve", "Alex"), hardNames(), "Steve got there first");
    }

    @Test
    default void topThree_aPlayerWhoReachesHisBestTwice_isDatedByTheFirstTime() {
        store().append(run(ALEX, "Alex", Mode.HARD, 60, 30));
        store().append(run(ALEX, "Alex", Mode.HARD, 60, 10));
        store().append(run(STEVE, "Steve", Mode.HARD, 60, 20));

        assertEquals(List.of("Alex", "Steve"), hardNames(), "Alex first reached 60 before Steve did");
        assertEquals(BASE.plusSeconds(10), store().topThreeOfEveryMode().get(Mode.HARD).entries().getFirst().achievedAt(), "dated by the first time");
    }

    @Test
    default void topThree_nameComesFromThePlayersLatestRun() {
        store().append(run(ALEX, "Alex", Mode.HARD, 88, 0));
        store().append(run(ALEX, "Alexander", Mode.EASY, 3, 3600));

        TopEntry first = store().topThreeOfEveryMode().get(Mode.HARD).entries().getFirst();
        assertEquals("Alexander", first.name(), "the name Alex has now, even on a run from before the rename");
        assertEquals(88, first.score(), "the score is still the best run's");
    }

    @Test
    default void topThree_keepsTheModesApart() {
        store().append(run(ALEX, "Alex", Mode.HARD, 88, 0));
        store().append(run(STEVE, "Steve", Mode.EASY, 70, 1));

        Map<Mode, TopThree> top = store().topThreeOfEveryMode();
        assertEquals(List.of("Alex"), top.get(Mode.HARD).entries().stream().map(TopEntry::name).toList(), "only Hard runs in Hard");
        assertEquals(List.of("Steve"), top.get(Mode.EASY).entries().stream().map(TopEntry::name).toList(), "only Easy runs in Easy");
    }

    @Test
    default void topThree_modesWithoutRuns_areAbsent() {
        store().append(run(ALEX, "Alex", Mode.HARD, 88, 0));

        assertEquals(Set.of(Mode.HARD), store().topThreeOfEveryMode().keySet(), "no entry for a mode nobody ran");
    }

    @Test
    default void topThree_withoutAnyRun_isEmpty() {
        assertTrue(store().topThreeOfEveryMode().isEmpty(), "nothing stored, nothing ranked");
    }

    @Test
    default void topThree_onAFullTie_playerIdsAreOrderedUnsignedLikePostgres() {
        UUID highBitSet = new UUID(Long.MIN_VALUE, 0);
        UUID highBitClear = new UUID(1, 0);
        store().append(run(highBitSet, "Negative", Mode.HARD, 10, 0));
        store().append(run(highBitClear, "Positive", Mode.HARD, 10, 0));

        assertEquals(List.of("Positive", "Negative"), hardNames(), "uuid order is bytewise unsigned");
    }

    @Test
    default void topThree_nameOnEqualFinishTimes_isTheLaterAppendedRuns() {
        store().append(run(ALEX, "Alex", Mode.HARD, 10, 5));
        store().append(run(ALEX, "Alexander", Mode.HARD, 10, 5));

        assertEquals(List.of("Alexander"), hardNames(), "the run appended last wins a tie on finished_at");
    }
}
