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

import net.minestom.server.MinecraftServer;

import java.time.Duration;
import java.time.ZoneId;

/**
 * The constants that every part of the time handling agrees on.
 *
 * <p>The lobby has exactly one editorial time zone. Seasons, day time and the announced start of a
 * seasonal event are all told in {@link #EDITORIAL_ZONE}, no matter where the process runs or which
 * zone the operating system reports. Resolving it in one place keeps a redeployment to a different
 * host from silently moving the lobby's calendar.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class TitanTime {

    /**
     * The zone the lobby's calendar and clock are told in.
     *
     * <p>{@code Europe/Berlin} rather than a fixed offset: the zone carries the daylight saving
     * rules, so {@code 12:00} stays noon across both transitions without anyone touching a
     * configuration file (NFR-006).
     */
    public static final ZoneId EDITORIAL_ZONE = ZoneId.of("Europe/Berlin");

    /**
     * How often the day time is pushed to an instance.
     *
     * <p>One Minecraft day is {@value DayTimeStrategy#TICKS_PER_DAY} ticks over 24 real hours, so a
     * single tick of game time lasts 3.6 real seconds. Updating once per second is therefore
     * already
     * finer than the value can change, and updating per server tick would send twenty identical
     * packets for every one that carries new information (NFR-008, US-2.14).
     */
    public static final Duration UPDATE_INTERVAL = Duration.ofSeconds(1);

    /**
     * Returns {@link #UPDATE_INTERVAL} counted in server ticks.
     *
     * <p>Minestom's scheduler takes either a wall-clock duration or a number of ticks. Ticks are
     * the
     * better unit here: a lagging server should update its day time less often, not pile up work it
     * cannot do, and a schedule expressed in ticks is what a test can advance by hand.
     *
     * @return the update interval in server ticks, at least one
     */
    public static int updateIntervalTicks() {
        long ticks = UPDATE_INTERVAL.toMillis() / MinecraftServer.TICK_MS;
        return (int) Math.max(1L, ticks);
    }

    private TitanTime() {
    }
}
