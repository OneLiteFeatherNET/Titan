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
package net.onelitefeather.titan.common.season;

import net.onelitefeather.titan.common.feature.SeasonWindowActivationStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.togglz.core.repository.FeatureState;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * When a season runs: an inclusive start, an exclusive end and the zone both are read in.
 *
 * <p>The window is not evaluated here. It is written onto a {@link FeatureState} and handed to
 * {@link SeasonWindowActivationStrategy}, the strategy the release gate already uses — so a season
 * and a feature flag answer "is it time yet" with the same code, including the same treatment of
 * summer time and the same refusal to open on an unreadable date. Duplicating the comparison would
 * have been three lines and a second thing to keep correct across a daylight-saving change.
 *
 * <p>Both bounds are optional. A window with only a start never closes, one with only an end was
 * always open, and one with neither is always open — which is how a season that is meant to be
 * switched by its kill switch alone is written.
 *
 * @param from inclusive start, {@code null} when the season has always been running
 * @param to   exclusive end, {@code null} when the season never ends on its own
 * @param zone the zone {@code from} and {@code to} are read in
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record SeasonWindow(@Nullable LocalDateTime from, @Nullable LocalDateTime to, ZoneId zone) {

    /**
     * Rejects a window that ends before it starts — the one mistake in a season file that would
     * otherwise produce a season nobody ever sees and no error anywhere.
     */
    public SeasonWindow {
        if (zone == null) {
            throw new IllegalArgumentException("a season window needs a zone");
        }
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("window ends at " + to + ", which is not after its start " + from);
        }
    }

    /**
     * Creates a window that is always open, planned in the given zone.
     *
     * @param zone the zone the season is planned in
     * @return a window with neither bound set
     */
    @Contract(value = "_ -> new", pure = true)
    public static SeasonWindow always(ZoneId zone) {
        return new SeasonWindow(null, null, zone);
    }

    /**
     * Writes this window onto a feature state, in the parameters
     * {@link SeasonWindowActivationStrategy} reads.
     *
     * @param state the state to write to
     * @return the same state, for chaining
     */
    @Contract("_ -> param1")
    public FeatureState applyTo(FeatureState state) {
        state.setStrategyId(SeasonWindowActivationStrategy.ID);
        state.setParameter(SeasonWindowActivationStrategy.PARAM_ZONE, this.zone.getId());
        if (this.from != null) {
            state.setParameter(SeasonWindowActivationStrategy.PARAM_FROM, this.from.toString());
        }
        if (this.to != null) {
            state.setParameter(SeasonWindowActivationStrategy.PARAM_TO, this.to.toString());
        }
        return state;
    }

    /**
     * Returns whether this window has an end at all.
     *
     * @return whether {@link #to()} is set
     */
    @Contract(pure = true)
    public boolean hasEnd() {
        return this.to != null;
    }
}
