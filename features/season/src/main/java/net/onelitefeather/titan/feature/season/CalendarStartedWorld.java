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

import io.avaje.inject.Secondary;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link StartedWorld}: evaluates the calendar once when the column starts. Also the
 * place where an invalid enabled season aborts startup.
 */
@Singleton
@Secondary
final class CalendarStartedWorld implements StartedWorld {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarStartedWorld.class);

    private final Optional<String> worldName;

    CalendarStartedWorld(SeasonSchedule schedule) {
        Optional<Season> season = schedule.atStartup();
        this.worldName = season.map(Season::world);
        season.ifPresentOrElse(active -> LOGGER.info("Lobby world {} (season {})", active.world(), active.id()), () -> LOGGER.info("Lobby world is the default world (no active season)"));
    }

    @Override
    public Optional<String> worldName() {
        return this.worldName;
    }
}
