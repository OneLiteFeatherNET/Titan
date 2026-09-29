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

import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import net.onelitefeather.titan.core.module.LobbyWorldChoice;

/**
 * Tells the platform which world to load when a season's window is open. Public with a no-arg
 * constructor only because the service loader instantiates it before any bean exists; an invalid
 * enabled season aborts startup here just as it does in the column.
 */
public final class SeasonWorldChoice implements LobbyWorldChoice {

    private final SeasonSchedule schedule;

    public SeasonWorldChoice() {
        this(Clock.systemUTC(), Path.of(SeasonSettings.WORLDS_DIRECTORY));
    }

    SeasonWorldChoice(Clock clock, Path worldsDirectory) {
        this.schedule = new SeasonSchedule(clock, worldsDirectory);
    }

    @Override
    public Optional<String> worldName() {
        return this.schedule.atStartup().map(Season::world);
    }
}
