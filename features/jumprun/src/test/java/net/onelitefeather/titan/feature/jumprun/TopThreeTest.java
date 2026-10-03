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

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TopThreeTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static UUID id(long n) {
        return new UUID(0, n);
    }

    private static TopEntry entry(long player, int score, long secondsAfterT0) {
        return new TopEntry(id(player), "p" + player, score, T0.plusSeconds(secondsAfterT0));
    }

    private static List<Integer> scores(TopThree top) {
        return top.entries().stream().map(TopEntry::score).toList();
    }

    @Test
    void entriesAreSortedDescendingByScore() {
        TopThree top = TopThree.EMPTY.with(entry(1, 10, 0)).with(entry(2, 30, 0)).with(entry(3, 20, 0));
        assertEquals(List.of(30, 20, 10), scores(top), "highest score first");
    }

    @Test
    void atMostThreeEntriesAreKept() {
        TopThree top = TopThree.EMPTY.with(entry(1, 10, 0)).with(entry(2, 20, 0)).with(entry(3, 30, 0)).with(entry(4, 40, 0));
        assertEquals(List.of(40, 30, 20), scores(top), "the lowest falls out");
    }

    @Test
    void aHigherScoreOfTheSamePlayerReplacesTheOldEntry() {
        TopThree top = TopThree.EMPTY.with(entry(1, 10, 0)).with(entry(2, 20, 0)).with(entry(1, 30, 5));
        assertEquals(List.of(30, 20), scores(top), "one entry per player");
        assertEquals(id(1), top.entries().getFirst().player(), "the player moves up");
    }

    @Test
    void aLowerOrEqualScoreOfTheSamePlayerChangesNothing() {
        TopThree top = TopThree.EMPTY.with(entry(1, 20, 0));
        assertSame(top, top.with(entry(1, 10, 1)), "lower score is ignored");
        assertSame(top, top.with(entry(1, 20, 1)), "equal score keeps the earlier time");
    }

    @Test
    void aTieIsWonByTheEarlierAchievement() {
        TopThree top = TopThree.EMPTY.with(entry(1, 20, 10)).with(entry(2, 20, 5));
        assertEquals(id(2), top.entries().getFirst().player(), "earlier achievedAt first");
    }

    @Test
    void anEntryTooLowForAFullListReturnsTheSameInstance() {
        TopThree top = TopThree.EMPTY.with(entry(1, 30, 0)).with(entry(2, 20, 0)).with(entry(3, 10, 0));
        assertSame(top, top.with(entry(4, 5, 0)), "unchanged input returns this");
    }

    @Test
    void emptyHasNoEntries() {
        assertEquals(List.of(), TopThree.EMPTY.entries(), "no entries");
    }

    @Test
    void equalScoreAndTimeAreOrderedByPlayerId() {
        TopThree forward = TopThree.EMPTY.with(entry(1, 10, 0)).with(entry(2, 10, 0));
        TopThree backward = TopThree.EMPTY.with(entry(2, 10, 0)).with(entry(1, 10, 0));

        assertEquals(forward, backward, "arrival order must not decide a full tie");
        assertEquals(id(1), forward.entries().getFirst().player(), "the lower player id ranks first");
    }

    @Test
    void playerIdsAreComparedUnsignedLikePostgresDoes() {
        UUID highBitSet = new UUID(Long.MIN_VALUE, 0);
        UUID highBitClear = new UUID(1, 0);
        TopEntry negative = new TopEntry(highBitSet, "negative", 10, T0);
        TopEntry positive = new TopEntry(highBitClear, "positive", 10, T0);

        TopThree top = TopThree.EMPTY.with(negative).with(positive);

        assertEquals(List.of(positive, negative), top.entries(), "bytewise unsigned: 0x80.. sorts after 0x00..01");
    }
}
