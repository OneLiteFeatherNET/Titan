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
package net.onelitefeather.titan.feature.npc;

import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SoundScheduleTest {

    @DisplayName("The first sound of a player comes after one interval, not at once")
    @Test
    void firstPlayIsAfterOneInterval() {
        SoundSchedule schedule = new SoundSchedule(4, 4, new Random(1));
        UUID player = UUID.randomUUID();
        Assertions.assertFalse(schedule.due(player, 0), "the first check only schedules the sound");
        Assertions.assertFalse(schedule.due(player, 3), "three takts into a four takt interval is too early");
        Assertions.assertTrue(schedule.due(player, 4), "after the interval the sound plays");
    }

    @DisplayName("Each play schedules the next one within the bounds")
    @Test
    void nextPlayStaysWithinBounds() {
        SoundSchedule schedule = new SoundSchedule(8, 12, new Random(3));
        UUID player = UUID.randomUUID();
        schedule.due(player, 0);
        long last = 0;
        int plays = 0;
        for (long takt = 1; takt <= 500; takt++) {
            if (schedule.due(player, takt)) {
                Assertions.assertTrue(takt - last >= 8 && takt - last <= 12, "the gap " + (takt - last) + " must be within 8..12 takts");
                last = takt;
                plays++;
            }
        }
        Assertions.assertTrue(plays > 30, "a sound plays repeatedly over 500 takts, was " + plays);
    }

    @DisplayName("Players have their own schedules")
    @Test
    void schedulesArePerPlayer() {
        SoundSchedule schedule = new SoundSchedule(4, 4, new Random(1));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        schedule.due(first, 0);
        schedule.due(second, 2);
        Assertions.assertTrue(schedule.due(first, 4), "the first player's sound is due at takt 4");
        Assertions.assertFalse(schedule.due(second, 4), "the second player's sound is not due until takt 6");
    }

    @DisplayName("Bounds must be positive and ordered")
    @Test
    void invalidBoundsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SoundSchedule(0, 4, new Random()), "a zero interval would play every takt");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SoundSchedule(8, 4, new Random()), "a minimum above the maximum is no interval");
    }
}
