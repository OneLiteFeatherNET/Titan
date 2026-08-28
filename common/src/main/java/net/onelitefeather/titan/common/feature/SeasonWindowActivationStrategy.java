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
package net.onelitefeather.titan.common.feature;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.togglz.core.activation.Parameter;
import org.togglz.core.activation.ParameterBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.spi.ActivationStrategy;
import org.togglz.core.user.FeatureUser;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Togglz activation strategy that limits a feature to a time window with a start, an end and a
 * time zone.
 *
 * <p>Togglz ships {@code ReleaseDateActivationStrategy}, but it only knows {@code PARAM_DATE} and
 * {@code PARAM_TIME} — a point in time after which a feature is on. A season has an end as well,
 * and it is planned in local Berlin time rather than in whatever zone the JVM happens to run in.
 * This strategy therefore takes three parameters:
 *
 * <ul>
 * <li>{@value #PARAM_FROM} — inclusive start, {@code 2026-10-01T18:00} or {@code 2026-10-01}</li>
 * <li>{@value #PARAM_TO} — exclusive end, same formats</li>
 * <li>{@value #PARAM_ZONE} — zone the two local times are read in, for example
 * {@code Europe/Berlin}; falls back to the zone this strategy was built with</li>
 * </ul>
 *
 * <p>Both bounds are optional: a window with only {@value #PARAM_FROM} never closes, one with only
 * {@value #PARAM_TO} was always open, and a state with neither is always within its window. A
 * parameter that is present but unreadable makes the feature inactive — a typo in a date must not
 * widen an audience.
 *
 * <p>Registered through {@code META-INF/services/org.togglz.core.spi.ActivationStrategy}, which
 * Togglz's {@code DefaultActivationStrategyProvider} reads with a plain
 * {@link java.util.ServiceLoader#load(Class)}; that call uses the thread context classloader, so
 * the lookup has to happen on a thread prepared by
 * {@link net.onelitefeather.titan.common.utils.ThreadHelper}. The public no-argument constructor
 * exists for that service lookup.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class SeasonWindowActivationStrategy implements ActivationStrategy {

    /** Strategy id as written to {@code <feature>.strategy} in the flag file. */
    public static final String ID = "season-window";

    /** Parameter holding the inclusive start of the window. */
    public static final String PARAM_FROM = "from";

    /** Parameter holding the exclusive end of the window. */
    public static final String PARAM_TO = "to";

    /** Parameter holding the zone the two local times are interpreted in. */
    public static final String PARAM_ZONE = "zone";

    /** Zone seasons are planned in when a feature does not name one. */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonWindowActivationStrategy.class);

    private static final Parameter[] PARAMETERS = {ParameterBuilder.create(PARAM_FROM).label("Window start").description("Inclusive start, ISO-8601 local date or date-time (2026-10-01 or 2026-10-01T18:00).").optional(), ParameterBuilder.create(PARAM_TO).label("Window end").description("Exclusive end, ISO-8601 local date or date-time (2026-11-05 or 2026-11-05T04:00).").optional(), ParameterBuilder.create(PARAM_ZONE).label("Time zone").description("Zone the two local times are read in, for example Europe/Berlin.").optional(),
    };

    private final Clock clock;
    private final ZoneId fallbackZone;

    /**
     * Creates the strategy the way the {@link java.util.ServiceLoader} needs it: on the system
     * clock, planning seasons in {@link #DEFAULT_ZONE}.
     */
    public SeasonWindowActivationStrategy() {
        this(Clock.systemUTC(), DEFAULT_ZONE);
    }

    /**
     * Creates the strategy with an explicit time source.
     *
     * @param clock        the clock every comparison is made against
     * @param fallbackZone the zone used for features that do not set {@value #PARAM_ZONE}
     */
    public SeasonWindowActivationStrategy(Clock clock, ZoneId fallbackZone) {
        this.clock = clock;
        this.fallbackZone = fallbackZone;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Season time window";
    }

    @Override
    public boolean isActive(FeatureState featureState, @Nullable FeatureUser user) {
        return isWithinWindow(featureState);
    }

    @Override
    public Parameter[] getParameters() {
        return PARAMETERS.clone();
    }

    /**
     * Checks whether the current time falls into the window configured on the feature state. The
     * user plays no part in this decision, which is why {@link FeatureGate} can call this directly
     * as the third and last step of its evaluation.
     *
     * @param featureState the state carrying the window parameters
     * @return whether now is inside the window; {@code false} when a parameter cannot be read
     */
    public boolean isWithinWindow(FeatureState featureState) {
        try {
            ZoneId zone = zoneOf(featureState);
            LocalDateTime from = parse(featureState.getParameter(PARAM_FROM));
            LocalDateTime to = parse(featureState.getParameter(PARAM_TO));
            ZonedDateTime now = ZonedDateTime.ofInstant(this.clock.instant(), zone);
            if (from != null && now.isBefore(from.atZone(zone))) {
                return false;
            }
            return to == null || now.isBefore(to.atZone(zone));
        } catch (DateTimeException exception) {
            LOGGER.warn("Feature {} has an unreadable season window (from={}, to={}, zone={}); treating it as inactive", featureState.getFeature().name(), featureState.getParameter(PARAM_FROM), featureState.getParameter(PARAM_TO), featureState.getParameter(PARAM_ZONE), exception);
            return false;
        }
    }

    /**
     * Reads the inclusive start of the window.
     *
     * @param featureState the state to read from
     * @return the start, or an empty optional when unset or unreadable
     */
    @Contract(pure = true)
    public Optional<LocalDateTime> from(FeatureState featureState) {
        return parseQuietly(featureState.getParameter(PARAM_FROM));
    }

    /**
     * Reads the exclusive end of the window.
     *
     * @param featureState the state to read from
     * @return the end, or an empty optional when unset or unreadable
     */
    @Contract(pure = true)
    public Optional<LocalDateTime> to(FeatureState featureState) {
        return parseQuietly(featureState.getParameter(PARAM_TO));
    }

    /**
     * Resolves the zone the window of this feature is planned in.
     *
     * @param featureState the state to read from
     * @return the configured zone, or the fallback zone when none is set
     * @throws DateTimeException when the configured zone id is not a known zone
     */
    @Contract(pure = true)
    public ZoneId zoneOf(FeatureState featureState) {
        String zone = featureState.getParameter(PARAM_ZONE);
        return zone == null || zone.isBlank() ? this.fallbackZone : ZoneId.of(zone.trim());
    }

    private Optional<LocalDateTime> parseQuietly(@Nullable String raw) {
        try {
            return Optional.ofNullable(parse(raw));
        } catch (DateTimeException exception) {
            return Optional.empty();
        }
    }

    private static @Nullable LocalDateTime parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.indexOf('T') < 0) {
            return LocalDate.parse(value).atStartOfDay();
        }
        return LocalDateTime.parse(value);
    }
}
