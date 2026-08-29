/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.time;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.onelitefeather.titan.common.time.season.SeasonBoundaryStrategy;
import net.theevilreaper.aves.file.ModernGsonFileHandler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Loads {@value TimeConfig#TIME_FILE_NAME} and hands out the two services built from it.
 *
 * <p>The point of the two {@code create…} methods is that they are the only place the strategies
 * are chosen. The application asks this provider for its services and never names a strategy, so
 * "which mapping runs" really is a configuration question and not a code question (US-2.07,
 * US-2.12, US-2.13).
 *
 * <p>Both values are resolved in the constructor rather than on first use. A misspelt strategy
 * therefore stops the boot, at the point where an operator is still watching the log — not an hour
 * later, and not silently never.
 *
 * <p>The {@link Clock} is not part of the configuration and never becomes part of it: it is passed
 * into {@link #createWorldTimeService(Clock)} and {@link #createSeasonService(Clock)} by the
 * caller, which is what keeps every mapping testable against a fixed instant (US-2.03).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class TimeConfigProvider {

    private static final TypeToken<TimeConfigImpl> TYPE = TypeToken.get(TimeConfigImpl.class);

    private final Path file;
    private final ZoneId zone;
    private final ModernGsonFileHandler fileHandler;
    private final TimeConfig timeConfig;
    private final DayTimeStrategy dayTimeStrategy;
    private final SeasonBoundaryStrategy seasonStrategy;

    private TimeConfigProvider(Path path, ZoneId zone) {
        this.file = path.resolve(TimeConfig.TIME_FILE_NAME);
        this.zone = zone;
        Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
        this.fileHandler = new ModernGsonFileHandler(gson);
        this.timeConfig = this.loadConfig();
        this.dayTimeStrategy = this.timeConfig.resolveDayTimeStrategy();
        this.seasonStrategy = this.timeConfig.resolveSeasonStrategy(zone);
    }

    /**
     * Creates a provider reading from the given directory, for the lobby's editorial zone.
     *
     * @param path the directory holding {@value TimeConfig#TIME_FILE_NAME}
     * @return the provider, with both strategies already resolved
     * @throws TimeConfigException if the file names a strategy or season this lobby does not know
     */
    @Contract(value = "_ -> new")
    @NotNull
    public static TimeConfigProvider create(@NotNull Path path) {
        return new TimeConfigProvider(path, TitanTime.EDITORIAL_ZONE);
    }

    /**
     * Creates a provider reading from the given directory, for the given zone.
     *
     * @param path the directory holding {@value TimeConfig#TIME_FILE_NAME}
     * @param zone the zone the day time and the season boundaries are calculated against
     * @return the provider, with both strategies already resolved
     * @throws TimeConfigException if the file names a strategy or season this lobby does not know
     */
    @Contract(value = "_, _ -> new")
    @NotNull
    public static TimeConfigProvider create(@NotNull Path path, @NotNull ZoneId zone) {
        return new TimeConfigProvider(path, zone);
    }

    /**
     * Returns the loaded configuration, as written in the file.
     *
     * @return the time configuration
     */
    @Contract(pure = true)
    @NotNull
    public TimeConfig getTimeConfig() {
        return this.timeConfig;
    }

    /**
     * Returns the zone both services are built for.
     *
     * @return the zone
     */
    @Contract(pure = true)
    @NotNull
    public ZoneId zone() {
        return this.zone;
    }

    /**
     * Returns the configured mapping from real time to day time.
     *
     * @return the resolved day time strategy
     */
    @Contract(pure = true)
    @NotNull
    public DayTimeStrategy dayTimeStrategy() {
        return this.dayTimeStrategy;
    }

    /**
     * Returns the configured season boundaries.
     *
     * @return the resolved season boundary strategy
     */
    @Contract(pure = true)
    @NotNull
    public SeasonBoundaryStrategy seasonStrategy() {
        return this.seasonStrategy;
    }

    /**
     * Builds the world time service the configuration asks for.
     *
     * @param clock the clock the current instant is read from
     * @return a service running the configured mapping in the configured zone
     */
    @Contract(value = "_ -> new")
    @NotNull
    public WorldTimeService createWorldTimeService(@NotNull Clock clock) {
        return WorldTimeService.create(clock, this.zone, this.dayTimeStrategy);
    }

    /**
     * Builds the season service the configuration asks for.
     *
     * @param clock the clock the current date is read from
     * @return a service running the configured boundaries in the configured zone
     */
    @Contract(value = "_ -> new")
    @NotNull
    public SeasonService createSeasonService(@NotNull Clock clock) {
        return SeasonService.create(clock, this.zone, this.seasonStrategy);
    }

    private TimeConfig loadConfig() {
        Optional<TimeConfigImpl> loaded;
        try {
            loaded = this.fileHandler.load(this.file, TYPE);
        } catch (RuntimeException exception) {
            // Unlike a missing file, a broken one is not a statement of intent. Falling back would
            // run a strategy nobody chose, and a wrong season is invisible for weeks.
            throw TimeConfigException.unreadable(this.file, exception);
        }
        if (loaded.isEmpty()) {
            // No file yet: keep the stage 2 defaults and write them out, so the next operator to
            // look sees both keys and their current values instead of an empty directory.
            this.fileHandler.save(this.file, TimeConfigImpl.DEFAULT, TYPE);
            return TimeConfig.defaultConfig();
        }
        return loaded.get();
    }
}
