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
package net.onelitefeather.titan.feature.season;

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Answers "which season's world should the lobby run in now" from the configuration and the
 * clock. Plain and free of server state, so anything that needs the answer, at startup or on a
 * later check, can share it.
 */
@Singleton
final class SeasonSchedule {

    /** The outcome of a live check. */
    sealed interface Desired {

        /** The configuration is usable; empty means the default world. */
        record Chosen(Optional<Season> season) implements Desired {
        }

        /** The configuration is currently invalid: it must trigger neither a restart nor a stop. */
        record Unresolvable() implements Desired {
        }
    }

    private final SeasonConfigReader reader;
    private final SeasonCalendar calendar;
    private final Clock clock;

    @Inject
    SeasonSchedule(Clock clock) {
        this(clock, Path.of(SeasonSettings.WORLDS_DIRECTORY));
    }

    SeasonSchedule(Clock clock, Path worldsDirectory) {
        this(clock, worldsDirectory, Config.asConfiguration());
    }

    SeasonSchedule(Clock clock, Path worldsDirectory, Configuration config) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.calendar = new SeasonCalendar();
        this.reader = new SeasonConfigReader(config, worldsDirectory, this.calendar);
    }

    /**
     * @throws IllegalStateException if an enabled season is invalid; startup must abort
     */
    Optional<Season> atStartup() {
        SeasonConfigReader.SeasonConfig config = this.reader.readAtStartup();
        return this.calendar.activeAt(config.seasons(), this.clock.instant(), config.zone());
    }

    Desired now() {
        return this.reader.readLive().<Desired>map(config -> new Desired.Chosen(this.calendar.activeAt(config.seasons(), this.clock.instant(), config.zone()))).orElseGet(Desired.Unresolvable::new);
    }
}
