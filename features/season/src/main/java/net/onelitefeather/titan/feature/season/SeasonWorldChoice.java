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
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import net.onelitefeather.titan.core.module.LobbyAtmosphere;
import net.onelitefeather.titan.core.module.LobbyWorldChoice;

/**
 * Tells the platform which world to load when a season's window is open, and which atmosphere
 * that season carries. Public with a no-arg constructor only because the service loader
 * instantiates it before any bean exists; an invalid enabled season aborts startup here just as
 * it does in the column.
 */
public final class SeasonWorldChoice implements LobbyWorldChoice {

    private final SeasonSchedule schedule;
    private final SeasonAtmospheres atmospheres;

    public SeasonWorldChoice() {
        this(Clock.systemUTC(), Path.of(SeasonSettings.WORLDS_DIRECTORY), Config.asConfiguration());
    }

    SeasonWorldChoice(Clock clock, Path worldsDirectory) {
        this(clock, worldsDirectory, Config.asConfiguration());
    }

    SeasonWorldChoice(Clock clock, Path worldsDirectory, Configuration config) {
        this.schedule = new SeasonSchedule(clock, worldsDirectory, config);
        this.atmospheres = new SeasonAtmospheres(config);
    }

    @Override
    public Optional<String> worldName() {
        return this.schedule.atStartup().map(Season::world);
    }

    @Override
    public Optional<LobbyAtmosphere> atmosphere() {
        return this.schedule.atStartup().flatMap(season -> this.atmospheres.of(season.id()));
    }
}
