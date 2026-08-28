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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers the release stage each feature was last seen on and logs every change.
 *
 * <p>Stages live in a flag file that is reloaded in the background, so a stage change is not an
 * event anyone fires — it is a difference between two observations. This class turns that
 * difference into one log line with timestamp, old stage and new stage (US-3.09). The first
 * observation of a feature is not a change: it seeds the memory and stays silent, so a restart
 * does not fake a transition.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class StageTransitionLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageTransitionLogger.class);

    private final Map<String, ReleaseStage> lastSeen = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ZoneId zone;

    /**
     * Creates a logger that timestamps transitions with the given clock.
     *
     * @param clock the time source, so tests do not have to wait for real time
     * @param zone  the zone timestamps are rendered in
     */
    public StageTransitionLogger(Clock clock, ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    /**
     * Records the stage a feature is currently on and reports a change against the previous
     * observation.
     *
     * @param feature name of the feature
     * @param stage   the stage observed now
     * @return the transition when the stage changed, otherwise an empty optional
     */
    public Optional<StageTransition> observe(String feature, ReleaseStage stage) {
        ReleaseStage previous = this.lastSeen.put(feature, stage);
        if (previous == null || previous == stage) {
            return Optional.empty();
        }
        StageTransition transition = new StageTransition(feature, previous, stage, ZonedDateTime.ofInstant(this.clock.instant(), this.zone));
        LOGGER.info("Feature {} changed release stage at {}: {} -> {}", transition.feature(), transition.at(), transition.from().id(), transition.to().id());
        return Optional.of(transition);
    }
}
