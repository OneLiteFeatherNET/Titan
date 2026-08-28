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

import org.jetbrains.annotations.Contract;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Maps an instant of the real world onto the day time of a Minecraft world.
 *
 * <p>Implementations are stateless and pure: they are handed the instant instead of reading a clock
 * themselves. The {@link java.time.Clock} lives in the calling {@link WorldTimeService} (US-2.03),
 * which is what makes every mapping testable against fixed instants without waiting for real time.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface DayTimeStrategy {

    /** A full Minecraft day in ticks. */
    int TICKS_PER_DAY = 24_000;

    /**
     * The tick at which the sun stands at its highest point in Minecraft.
     *
     * <p>Minecraft counts a day from sunrise: tick {@code 0} is daybreak, {@code 6000} noon,
     * {@code 12000} nightfall and {@code 18000} midnight.
     */
    int NOON_TICK = 6_000;

    /** The first tick of the Minecraft night; the day segment is {@code [0, DUSK_TICK)}. */
    int DUSK_TICK = 12_000;

    /**
     * Returns the day time that applies at the given instant.
     *
     * @param instant the instant the day time applies to
     * @param zone    the time zone that is calculated against
     * @return the day time in ticks, within {@code [0, }{@value #TICKS_PER_DAY}{@code )}
     */
    @Contract(pure = true)
    long ticksAt(Instant instant, ZoneId zone);

    /**
     * Returns the linear mapping, the default of stage 2 (US-2.06).
     *
     * @return the shared, immutable linear strategy
     */
    @Contract(pure = true)
    static DayTimeStrategy linear() {
        return LinearDayTimeStrategy.instance();
    }

    /**
     * Returns the solar mapping for Berlin (US-2.07).
     *
     * @return the shared, immutable solar strategy for Berlin
     */
    @Contract(pure = true)
    static DayTimeStrategy solar() {
        return SolarDayTimeStrategy.berlin();
    }
}
