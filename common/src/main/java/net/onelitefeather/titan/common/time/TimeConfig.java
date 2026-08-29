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

import net.onelitefeather.titan.common.time.season.Season;
import net.onelitefeather.titan.common.time.season.SeasonBoundaryStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Which time and season strategy the lobby runs, as an operator can change it without a code
 * change (US-2.07, US-2.12, US-2.13).
 *
 * <p>This is the "where … is configured" the three acceptance criteria are written as: the calling
 * code knows only {@link DayTimeStrategy} and {@link SeasonBoundaryStrategy}, and this file decides
 * which implementation answers.
 *
 * <p>It follows the shape of {@link net.onelitefeather.titan.common.config.AppConfig} — a sealed
 * interface with a package-private record behind it, loaded from JSON by a provider — minus the
 * builder, the same way {@code PortalConfig} does. {@code AppConfig} has a builder because
 * {@code /app} edits it while the server runs. These two values are read once, when the services
 * are constructed at boot; a builder and a command would advertise a live switch that does not
 * exist.
 *
 * <h2>Unknown values are a startup failure, not a fallback</h2>
 *
 * A misspelt strategy name that quietly fell back to the default would leave the lobby running the
 * wrong season for up to a month, and nothing would look broken. So every value that is present
 * but not recognised throws a {@link TimeConfigException} naming both the offending value and the
 * accepted ones. An <em>absent</em> value is a different thing and keeps the stage 2 default:
 * linear day time (US-2.06) and meteorological seasons (US-2.11).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public sealed interface TimeConfig permits TimeConfigImpl {

    /** Name of the file the strategies are read from, next to {@code app.json}. */
    String TIME_FILE_NAME = "time.json";

    /** {@link #dayTimeStrategy()} value selecting {@link LinearDayTimeStrategy} — the default. */
    String LINEAR = "linear";

    /** {@link #dayTimeStrategy()} value selecting {@link SolarDayTimeStrategy} (US-2.07). */
    String SOLAR = "solar";

    /**
     * {@link #seasonStrategy()} value selecting the meteorological boundaries — the default.
     */
    String METEOROLOGICAL = "meteorological";

    /** {@link #seasonStrategy()} value selecting the astronomical boundaries (US-2.12). */
    String ASTRONOMICAL = "astronomical";

    /**
     * {@link #seasonStrategy()} value pinning the season named by {@link #fixedSeason()}
     * (US-2.13).
     */
    String FIXED = "fixed";

    /** The accepted {@link #dayTimeStrategy()} values, in the order an error message lists them. */
    List<String> DAY_TIME_STRATEGIES = List.of(LINEAR, SOLAR);

    /** The accepted {@link #seasonStrategy()} values, in the order an error message lists them. */
    List<String> SEASON_STRATEGIES = List.of(METEOROLOGICAL, ASTRONOMICAL, FIXED);

    /**
     * Returns the configuration used when no {@value #TIME_FILE_NAME} exists yet: the stage 2
     * defaults, spelled out rather than left empty. Writing this file out is what shows an operator
     * both keys and the values they already have.
     *
     * @return the default configuration
     */
    @Contract(pure = true)
    static TimeConfig defaultConfig() {
        return TimeConfigImpl.DEFAULT;
    }

    /**
     * Creates a configuration directly, for tests and for callers that assemble it in code.
     *
     * @param dayTimeStrategy the day time strategy name, or {@code null} for the default
     * @param seasonStrategy  the season strategy name, or {@code null} for the default
     * @param fixedSeason     the season name, read only when {@code seasonStrategy} is
     *                        {@value #FIXED}
     * @return a configuration with those values, still unresolved
     */
    @Contract(pure = true, value = "_, _, _ -> new")
    static TimeConfig of(@Nullable String dayTimeStrategy, @Nullable String seasonStrategy, @Nullable String fixedSeason) {
        return new TimeConfigImpl(dayTimeStrategy, seasonStrategy, fixedSeason);
    }

    /**
     * The configured day time strategy, as written in the file and not yet validated.
     *
     * @return {@value #LINEAR}, {@value #SOLAR}, {@code null} when the key is absent, or whatever
     *         else the file says
     */
    @Contract(pure = true)
    @Nullable
    String dayTimeStrategy();

    /**
     * The configured season strategy, as written in the file and not yet validated.
     *
     * @return {@value #METEOROLOGICAL}, {@value #ASTRONOMICAL}, {@value #FIXED}, {@code null} when
     *         the key is absent, or whatever else the file says
     */
    @Contract(pure = true)
    @Nullable
    String seasonStrategy();

    /**
     * The season {@value #FIXED} pins the lobby to, as written in the file and not yet validated.
     *
     * <p>Read only when {@link #seasonStrategy()} is {@value #FIXED}; a value left here while
     * another strategy runs is ignored rather than treated as an error, so that switching back and
     * forth does not mean deleting the key each time.
     *
     * @return the season identifier, or {@code null} when the key is absent
     */
    @Contract(pure = true)
    @Nullable
    String fixedSeason();

    /**
     * Resolves {@link #dayTimeStrategy()} into the implementation that will answer.
     *
     * @return the linear strategy when the value is absent or {@value #LINEAR}, the solar strategy
     *         when it is {@value #SOLAR}
     * @throws TimeConfigException if the value is present but not one of {@link
     *                             #DAY_TIME_STRATEGIES}
     */
    @Contract(pure = true)
    @NotNull
    default DayTimeStrategy resolveDayTimeStrategy() {
        String name = normalize(dayTimeStrategy());
        if (name == null) {
            return DayTimeStrategy.linear();
        }
        return switch (name) {
            case LINEAR -> DayTimeStrategy.linear();
            case SOLAR -> DayTimeStrategy.solar();
            default ->
                throw TimeConfigException.unknownValue("dayTimeStrategy", dayTimeStrategy(), DAY_TIME_STRATEGIES);
        };
    }

    /**
     * Resolves {@link #seasonStrategy()} into the implementation that will answer.
     *
     * @param zone the zone an astronomical boundary instant is resolved in; an equinox is an
     *             instant, not a date, so the zone decides which calendar day it lands on
     * @return the meteorological strategy when the value is absent or {@value #METEOROLOGICAL}, the
     *         astronomical one when it is {@value #ASTRONOMICAL}, and a strategy pinned to
     *         {@link #fixedSeason()} when it is {@value #FIXED}
     * @throws TimeConfigException if the value is present but not one of
     *                             {@link #SEASON_STRATEGIES}, or if {@value #FIXED} is asked for
     *                             without a usable {@link #fixedSeason()}
     */
    @Contract(pure = true)
    @NotNull
    default SeasonBoundaryStrategy resolveSeasonStrategy(@NotNull ZoneId zone) {
        String name = normalize(seasonStrategy());
        if (name == null) {
            return SeasonBoundaryStrategy.meteorological();
        }
        return switch (name) {
            case METEOROLOGICAL -> SeasonBoundaryStrategy.meteorological();
            case ASTRONOMICAL -> SeasonBoundaryStrategy.astronomical(zone);
            case FIXED -> SeasonBoundaryStrategy.fixed(resolveFixedSeason());
            default ->
                throw TimeConfigException.unknownValue("seasonStrategy", seasonStrategy(), SEASON_STRATEGIES);
        };
    }

    /**
     * Resolves {@link #fixedSeason()} into the season {@value #FIXED} pins the lobby to.
     *
     * @return the named season
     * @throws TimeConfigException if the value is absent, or is not the identifier of a
     *                             {@link Season}
     */
    @Contract(pure = true)
    @NotNull
    default Season resolveFixedSeason() {
        String name = normalize(fixedSeason());
        if (name == null) {
            throw TimeConfigException.missingFixedSeason(seasonIds());
        }
        for (Season season : Season.values()) {
            if (season.id().equals(name)) {
                return season;
            }
        }
        throw TimeConfigException.unknownValue("fixedSeason", fixedSeason(), seasonIds());
    }

    /**
     * The identifiers every {@link Season} is written as in {@value #TIME_FILE_NAME}.
     *
     * @return the season identifiers in calendar order
     */
    @Contract(pure = true)
    @NotNull
    static List<String> seasonIds() {
        return Arrays.stream(Season.values()).map(Season::id).toList();
    }

    /**
     * Lower-cases and trims a configured value so that {@code "Solar"} and {@code " solar "} are
     * the same value, while a blank string stays distinct from an absent key and still fails.
     *
     * @param value the value as written in the file
     * @return the comparable form, or {@code null} if the key was absent
     */
    @Contract(pure = true, value = "null -> null; !null -> !null")
    private static String normalize(@Nullable String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
