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
 * Picks the next title from the catalog, always inside one category: the title after the ended one
 * in order, the same title again, or a random other title, depending on the play mode. The
 * randomness is injected, so a test fixes the seed.
 */
final class RadioSelection {

    private final List<RadioSong> catalog;
    private final RandomGenerator random;

    /** The catalog in menu order, each title carrying its category. */
    RadioSelection(List<RadioSong> catalog, RandomGenerator random) {
        this.catalog = List.copyOf(Objects.requireNonNull(catalog, "catalog must not be null"));
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * The title that starts when {@code ended} ends, inside its category, if the mode starts one.
     */
    Optional<RadioSong> afterEnd(RadioSong ended, PlayMode mode) {
        List<RadioSong> scope = inCategory(ended.category());
        if (scope.isEmpty()) {
            return Optional.empty();
        }
        return switch (mode) {
            case ONCE -> Optional.empty();
            case LOOP -> Optional.of(ended);
            case SHUFFLE -> Optional.of(otherThan(ended, scope));
        };
    }

    /**
     * The title that the next button starts. With a title playing it stays in that title's
     * category; without one it starts in {@code lastOpened}, the category the player opened last.
     * Shuffle picks another title at random; the other modes go in order and wrap.
     */
    Optional<RadioSong> next(RadioSong current, RadioCategory lastOpened, PlayMode mode) {
        List<RadioSong> scope = current != null ? inCategory(current.category()) : inCategory(lastOpened);
        if (scope.isEmpty()) {
            return Optional.empty();
        }
        if (mode == PlayMode.SHUFFLE) {
            return Optional.of(otherThan(current, scope));
        }
        int index = current == null ? -1 : scope.indexOf(current);
        return Optional.of(scope.get((index + 1) % scope.size()));
    }

    private List<RadioSong> inCategory(RadioCategory category) {
        if (category == null) {
            return List.of();
        }
        return this.catalog.stream().filter(song -> song.category().id().equals(category.id())).toList();
    }

    // Uniform over the scope without the current title; with one title there is no other, so it repeats.
    private RadioSong otherThan(RadioSong current, List<RadioSong> scope) {
        int size = scope.size();
        if (size == 1) {
            return scope.getFirst();
        }
        int index = current == null ? -1 : scope.indexOf(current);
        if (index < 0) {
            return scope.get(this.random.nextInt(size));
        }
        int pick = this.random.nextInt(size - 1);
        return scope.get(pick >= index ? pick + 1 : pick);
    }
}
