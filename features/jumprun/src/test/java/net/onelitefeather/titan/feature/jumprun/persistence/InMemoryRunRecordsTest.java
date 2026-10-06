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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.OptionalInt;
import java.util.UUID;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import org.junit.jupiter.api.Test;

class InMemoryRunRecordsTest {

    private final RunRecords records = new InMemoryRunRecords();
    private final UUID player = UUID.randomUUID();

    private static FinishedRun run(UUID player, Mode mode, int score) {
        return new FinishedRun(player, "Alex", mode, score, EndReason.FALL, Instant.parse("2026-10-03T12:00:00Z"));
    }

    @Test
    void loadingChangesNothingInMemory() {
        records.submit(run(player, Mode.MEDIUM, 12));

        records.load(player);

        assertEquals(OptionalInt.of(12), records.best(player, Mode.MEDIUM), "load is a no-op without a store");
    }

    @Test
    void playerWithoutRunHasNoBest() {
        assertTrue(records.best(player, Mode.MEDIUM).isEmpty(), "no best before the first run");
    }

    @Test
    void firstRunIsARecord() {
        assertTrue(records.submit(run(player, Mode.MEDIUM, 12)), "first run is a record");
        assertEquals(OptionalInt.of(12), records.best(player, Mode.MEDIUM));
    }

    @Test
    void higherScoreIsANewRecord() {
        records.submit(run(player, Mode.MEDIUM, 12));
        assertTrue(records.submit(run(player, Mode.MEDIUM, 15)), "15 beats 12");
        assertEquals(OptionalInt.of(15), records.best(player, Mode.MEDIUM));
    }

    @Test
    void lowerScoreIsNoRecordAndKeepsTheBest() {
        records.submit(run(player, Mode.MEDIUM, 15));
        assertFalse(records.submit(run(player, Mode.MEDIUM, 9)), "9 does not beat 15");
        assertEquals(OptionalInt.of(15), records.best(player, Mode.MEDIUM));
    }

    @Test
    void equalScoreIsNoRecord() {
        records.submit(run(player, Mode.MEDIUM, 15));
        assertFalse(records.submit(run(player, Mode.MEDIUM, 15)), "a tie is not a record");
    }

    @Test
    void playersAreIndependent() {
        UUID other = UUID.randomUUID();
        records.submit(run(player, Mode.MEDIUM, 15));
        assertTrue(records.submit(run(other, Mode.MEDIUM, 3)), "other player's first run is a record");
        assertEquals(OptionalInt.of(15), records.best(player, Mode.MEDIUM));
        assertEquals(OptionalInt.of(3), records.best(other, Mode.MEDIUM));
    }

    @Test
    void forgettingRemovesTheBestSoTheNextScoreIsARecord() {
        records.submit(run(player, Mode.MEDIUM, 15));

        records.forget(player);

        assertTrue(records.best(player, Mode.MEDIUM).isEmpty(), "the best is gone");
        assertTrue(records.submit(run(player, Mode.MEDIUM, 1)), "any score is a record again");
    }

    @Test
    void forgettingOnePlayerKeepsTheOthers() {
        UUID other = UUID.randomUUID();
        records.submit(run(player, Mode.MEDIUM, 15));
        records.submit(run(other, Mode.MEDIUM, 3));

        records.forget(player);

        assertEquals(OptionalInt.of(3), records.best(other, Mode.MEDIUM));
    }

    @Test
    void recordsOfDifferentModesAreIndependent() {
        records.submit(run(player, Mode.EASY, 30));

        assertTrue(records.best(player, Mode.HARD).isEmpty(), "hard has no best yet");
        assertTrue(records.submit(run(player, Mode.HARD, 2)), "a low hard score is still the first hard record");
        assertEquals(OptionalInt.of(30), records.best(player, Mode.EASY));
        assertEquals(OptionalInt.of(2), records.best(player, Mode.HARD));
    }

    @Test
    void forgettingAPlayerDropsEveryMode() {
        records.submit(run(player, Mode.EASY, 30));
        records.submit(run(player, Mode.HARD, 2));

        records.forget(player);

        assertTrue(records.best(player, Mode.EASY).isEmpty() && records.best(player, Mode.HARD).isEmpty(), "no mode keeps a best");
    }
}
