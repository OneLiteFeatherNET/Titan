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

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Drives the day time of an instance from real time (US-2.01).
 *
 * <p>The service owns the two things a {@link DayTimeStrategy} deliberately does not: the
 * {@link Clock} and the instance. The clock is injected rather than read from
 * {@link java.time.Instant#now()} so that a test can stand on a fixed instant instead of waiting
 * for
 * one (US-2.03, NFR-007).
 *
 * <p>{@link #bind(Instance)} switches Minestom's own cycle off before it sets anything, because two
 * writers on the same value produce a lobby that flickers between them (US-2.02). From then on the
 * time is pushed once per second — never per tick, which would be twenty packets for every one that
 * carries a changed value (US-2.14, NFR-008).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface WorldTimeService {

    /**
     * Creates a service on the linear mapping in the editorial zone — the stage 2 default.
     *
     * @param clock the clock the current instant is read from
     * @return a new service
     */
    @Contract(pure = true, value = "_ -> new")
    static WorldTimeService create(Clock clock) {
        return create(clock, TitanTime.EDITORIAL_ZONE, DayTimeStrategy.linear());
    }

    /**
     * Creates a service on the given mapping in the editorial zone.
     *
     * @param clock    the clock the current instant is read from
     * @param strategy the mapping from real time to day time
     * @return a new service
     */
    @Contract(pure = true, value = "_, _ -> new")
    static WorldTimeService create(Clock clock, DayTimeStrategy strategy) {
        return create(clock, TitanTime.EDITORIAL_ZONE, strategy);
    }

    /**
     * Creates a service on the given mapping in the given zone.
     *
     * @param clock    the clock the current instant is read from
     * @param zone     the zone the mapping is calculated against
     * @param strategy the mapping from real time to day time
     * @return a new service
     */
    @Contract(pure = true, value = "_, _, _ -> new")
    static WorldTimeService create(Clock clock, ZoneId zone, DayTimeStrategy strategy) {
        return new TitanWorldTimeService(clock, zone, strategy);
    }

    /**
     * Returns the day time that applies at the clock's current instant.
     *
     * @return the day time in ticks, within
     *         {@code [0, }{@value DayTimeStrategy#TICKS_PER_DAY}{@code )}
     */
    long currentTicks();

    /**
     * Returns the zone the mapping is calculated against.
     *
     * @return the zone
     */
    @Contract(pure = true)
    ZoneId zone();

    /**
     * Returns the mapping in use.
     *
     * @return the strategy
     */
    @Contract(pure = true)
    DayTimeStrategy strategy();

    /**
     * Takes the instance over: stops Minestom's own day cycle, writes the current time once, and
     * schedules the repeating update.
     *
     * <p>Binding a second instance replaces the first; the previous update task is cancelled.
     *
     * @param instance the instance whose day time this service drives
     */
    void bind(Instance instance);

    /**
     * Cancels the update task and releases the instance. The day time is left where it was, and the
     * instance's own cycle stays off — resuming it is the caller's decision.
     */
    void unbind();

    /**
     * Writes the current day time to the bound instance.
     *
     * <p>Called by the scheduled task, and directly by tests. It is a no-op when nothing is bound
     * or
     * when it already ran during the current wall-clock second, which is what keeps the promise of
     * {@link TitanTime#UPDATE_INTERVAL} independent of how often anyone calls it.
     *
     * @return {@code true} if the time was written, {@code false} if the call was skipped
     */
    boolean update();

    /**
     * Returns the instance this service currently drives.
     *
     * @return the bound instance, or {@code null} if none is bound
     */
    @Contract(pure = true)
    @Nullable
    Instance boundInstance();
}
