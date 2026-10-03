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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** The best three players of a mode; immutable, every player at most once. */
record TopThree(List<TopEntry> entries) {

    static final int SIZE = 3;

    static final TopThree EMPTY = new TopThree(List.of());

    private static final Comparator<TopEntry> RANKING = Comparator.comparingInt(TopEntry::score).reversed().thenComparing(TopEntry::achievedAt).thenComparing(TopEntry::player, TopThree::compareUnsigned);

    // Postgres orders uuids bytewise unsigned; UUID.compareTo compares signed longs and would
    // rank the same players differently from the SQL ranking.
    private static int compareUnsigned(UUID a, UUID b) {
        int byHigh = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        return byHigh != 0 ? byHigh : Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }

    TopThree {
        entries = List.copyOf(entries);
    }

    /** Sorts the entry in; returns this instance when it changes nothing. */
    TopThree with(TopEntry entry) {
        boolean beaten = entries.stream().anyMatch(known -> known.player().equals(entry.player()) && known.score() >= entry.score());
        if (beaten) {
            return this;
        }
        List<TopEntry> next = new ArrayList<>(entries);
        next.removeIf(known -> known.player().equals(entry.player()));
        next.add(entry);
        next.sort(RANKING);
        List<TopEntry> top = List.copyOf(next.subList(0, Math.min(SIZE, next.size())));
        return top.equals(entries) ? this : new TopThree(top);
    }
}
