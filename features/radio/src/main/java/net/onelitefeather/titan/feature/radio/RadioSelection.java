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
package net.onelitefeather.titan.feature.radio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Picks the next title from the catalog: the title after the ended one in order, the same title
 * again, or a random other title, depending on the play mode. The randomness is injected, so a
 * test fixes the seed.
 */
final class RadioSelection {

    private final List<RadioSong> catalog;
    private final RandomGenerator random;

    /**
     * The catalog in menu order: the vanilla discs first, then the MIDI songs in configuration
     * order.
     */
    RadioSelection(List<RadioSong> catalog, RandomGenerator random) {
        this.catalog = List.copyOf(Objects.requireNonNull(catalog, "catalog must not be null"));
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /** The title that starts when {@code ended} ends, if the mode starts one. */
    Optional<RadioSong> afterEnd(RadioSong ended, PlayMode mode) {
        if (this.catalog.isEmpty()) {
            return Optional.empty();
        }
        return switch (mode) {
            case ONCE -> Optional.empty();
            case LOOP -> Optional.of(ended);
            case SHUFFLE -> Optional.of(otherThan(ended));
        };
    }

    /**
     * The title that the next button starts after {@code current}, which may be {@code null} when
     * nothing plays. Shuffle picks another title at random; the other modes go in order.
     */
    Optional<RadioSong> next(RadioSong current, PlayMode mode) {
        if (this.catalog.isEmpty()) {
            return Optional.empty();
        }
        if (mode == PlayMode.SHUFFLE) {
            return Optional.of(otherThan(current));
        }
        int index = current == null ? -1 : this.catalog.indexOf(current);
        return Optional.of(this.catalog.get((index + 1) % this.catalog.size()));
    }

    // Uniform over the catalog without the current title; with one title there is no other, so it repeats.
    private RadioSong otherThan(RadioSong current) {
        int size = this.catalog.size();
        if (size == 1) {
            return this.catalog.getFirst();
        }
        int index = current == null ? -1 : this.catalog.indexOf(current);
        if (index < 0) {
            return this.catalog.get(this.random.nextInt(size));
        }
        int pick = this.random.nextInt(size - 1);
        return this.catalog.get(pick >= index ? pick + 1 : pick);
    }
}
