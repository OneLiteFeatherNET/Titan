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

import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryRunRecordsTest {

    private final RunRecords records = new InMemoryRunRecords();
    private final UUID player = UUID.randomUUID();

    @Test
    void playerWithoutRunHasNoBest() {
        assertTrue(records.best(player).isEmpty(), "no best before the first run");
    }

    @Test
    void firstRunIsARecord() {
        assertTrue(records.submit(player, 12), "first run is a record");
        assertEquals(OptionalInt.of(12), records.best(player));
    }

    @Test
    void higherScoreIsANewRecord() {
        records.submit(player, 12);
        assertTrue(records.submit(player, 15), "15 beats 12");
        assertEquals(OptionalInt.of(15), records.best(player));
    }

    @Test
    void lowerScoreIsNoRecordAndKeepsTheBest() {
        records.submit(player, 15);
        assertFalse(records.submit(player, 9), "9 does not beat 15");
        assertEquals(OptionalInt.of(15), records.best(player));
    }

    @Test
    void equalScoreIsNoRecord() {
        records.submit(player, 15);
        assertFalse(records.submit(player, 15), "a tie is not a record");
    }

    @Test
    void playersAreIndependent() {
        UUID other = UUID.randomUUID();
        records.submit(player, 15);
        assertTrue(records.submit(other, 3), "other player's first run is a record");
        assertEquals(OptionalInt.of(15), records.best(player));
        assertEquals(OptionalInt.of(3), records.best(other));
    }

    @Test
    void forgettingRemovesTheBestSoTheNextScoreIsARecord() {
        records.submit(player, 15);

        records.forget(player);

        assertTrue(records.best(player).isEmpty(), "the best is gone");
        assertTrue(records.submit(player, 1), "any score is a record again");
    }

    @Test
    void forgettingOnePlayerKeepsTheOthers() {
        UUID other = UUID.randomUUID();
        records.submit(player, 15);
        records.submit(other, 3);

        records.forget(player);

        assertEquals(OptionalInt.of(3), records.best(other));
    }
}
