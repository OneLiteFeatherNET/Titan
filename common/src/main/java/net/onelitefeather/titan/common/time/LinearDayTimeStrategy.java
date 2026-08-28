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
 * Spreads 24 real hours evenly over the {@value DayTimeStrategy#TICKS_PER_DAY} ticks of a Minecraft
 * day, so that {@code 12:00} local time is noon in the world.
 *
 * <p>This is the default of stage 2 (US-2.06). It delivers nearly the whole benefit of a real-time
 * lobby and has no astronomical calculation that could be silently wrong.
 *
 * <p>The mapping reads the <em>local wall clock</em>, not the offset from UTC. That is deliberate:
 * on the last Sunday in March the lobby jumps forward by 1000 ticks together with everybody's
 * watch,
 * and on the last Sunday in October it repeats the hour. Noon in the world is whenever a player in
 * Berlin says it is noon (US-2.04, NFR-006).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class LinearDayTimeStrategy implements DayTimeStrategy {

    private static final LinearDayTimeStrategy INSTANCE = new LinearDayTimeStrategy();

    private static final int SECONDS_PER_DAY = 86_400;

    /**
     * The wall clock second that Minecraft tick {@code 0} stands for.
     *
     * <p>Tick {@code 0} is daybreak, which the game presents as 06:00.
     */
    private static final int DAYBREAK_SECOND_OF_DAY = 6 * 3_600;

    private LinearDayTimeStrategy() {
    }

    /**
     * Returns the shared instance.
     *
     * <p>The strategy carries no state, so a single instance serves every caller.
     *
     * @return the shared linear strategy
     */
    @Contract(pure = true)
    public static LinearDayTimeStrategy instance() {
        return INSTANCE;
    }

    @Override
    @Contract(pure = true)
    public long ticksAt(Instant instant, ZoneId zone) {
        int secondOfDay = instant.atZone(zone).toLocalTime().toSecondOfDay();
        long sinceDaybreak = Math.floorMod(secondOfDay - DAYBREAK_SECOND_OF_DAY, (long) SECONDS_PER_DAY);
        return sinceDaybreak * TICKS_PER_DAY / SECONDS_PER_DAY;
    }

    @Override
    public String toString() {
        return "LinearDayTimeStrategy";
    }
}
