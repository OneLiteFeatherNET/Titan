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

import io.avaje.inject.RequiresProperty;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.FinishedRun;
import net.onelitefeather.titan.feature.jumprun.persistence.RunStore;
import net.onelitefeather.titan.feature.jumprun.persistence.TopEntry;
import net.onelitefeather.titan.feature.jumprun.persistence.TopThree;
import net.onelitefeather.titan.persistence.DatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The top three of every mode, in memory. {@link #refresh()} reads the store and may block, so it
 * never runs on the tick thread; {@link #top} and {@link #offer} do not block. Gated like the
 * store, so there is no leaderboard without a database.
 */
@Singleton
@RequiresProperty(DatabaseProperties.URL)
class Leaderboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(Leaderboard.class);

    private final RunStore store;
    private final AtomicReference<Map<Mode, TopThree>> tops = new AtomicReference<>(Map.of());

    Leaderboard(RunStore store) {
        this.store = store;
    }

    /** The best three of {@code mode}; never an entry of another mode. */
    TopThree top(Mode mode) {
        return this.tops.get().getOrDefault(mode, TopThree.EMPTY);
    }

    /** Sorts a new best in at once, before the store has it. */
    void offer(FinishedRun run) {
        offer(run.player(), run.name(), run.mode(), run.score(), run.finishedAt());
    }

    /**
     * The same for a score reached in a run that has not ended, so there is no finished run yet.
     */
    void offer(UUID player, String name, Mode mode, int score, Instant at) {
        TopEntry entry = new TopEntry(player, name, score, at);
        this.tops.updateAndGet(current -> {
            Map<Mode, TopThree> next = new EnumMap<>(Mode.class);
            next.putAll(current);
            next.put(mode, topOf(current, mode).with(entry));
            return Map.copyOf(next);
        });
    }

    /**
     * Takes the store's top threes. Entries offered since, which the store does not have yet, are
     * sorted back in, so a refresh never takes a new record off the board.
     */
    void refresh() {
        Map<Mode, TopThree> stored;
        try {
            stored = this.store.topThreeOfEveryMode();
        } catch (RuntimeException failure) {
            LOGGER.atWarn().setCause(failure).log("Could not refresh jump and run leaderboard");
            return;
        }
        this.tops.updateAndGet(current -> {
            Map<Mode, TopThree> next = new EnumMap<>(Mode.class);
            next.putAll(stored);
            current.forEach((mode, local) -> {
                TopThree merged = topOf(next, mode);
                for (TopEntry entry : local.entries()) {
                    merged = merged.with(entry);
                }
                next.put(mode, merged);
            });
            return Map.copyOf(next);
        });
    }

    private static TopThree topOf(Map<Mode, TopThree> tops, Mode mode) {
        return tops.getOrDefault(mode, TopThree.EMPTY);
    }
}
