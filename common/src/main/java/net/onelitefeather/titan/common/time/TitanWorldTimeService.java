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
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The default {@link WorldTimeService}; reached through {@link WorldTimeService#create(Clock)}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
final class TitanWorldTimeService implements WorldTimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanWorldTimeService.class);

    /** Minestom's rate for "the clock does not advance on its own". */
    private static final float FROZEN_RATE = 0.0f;

    private final Clock clock;
    private final ZoneId zone;
    private final DayTimeStrategy strategy;

    private @Nullable Instance instance;
    private @Nullable Task task;

    /** The wall-clock second the last write happened in; {@link Long#MIN_VALUE} for "never". */
    private long lastWrittenSecond = Long.MIN_VALUE;

    TitanWorldTimeService(Clock clock, ZoneId zone, DayTimeStrategy strategy) {
        this.clock = clock;
        this.zone = zone;
        this.strategy = strategy;
    }

    @Override
    public long currentTicks() {
        return this.strategy.ticksAt(this.clock.instant(), this.zone);
    }

    @Override
    public ZoneId zone() {
        return this.zone;
    }

    @Override
    public DayTimeStrategy strategy() {
        return this.strategy;
    }

    @Override
    public void bind(Instance instance) {
        unbind();
        this.instance = instance;
        freezeOwnCycle(instance);
        this.lastWrittenSecond = Long.MIN_VALUE;
        update();
        TaskSchedule interval = TaskSchedule.tick(TitanTime.updateIntervalTicks());
        this.task = instance.scheduler().buildTask(this::update).delay(interval).repeat(interval).schedule();
        LOGGER.info("Driving day time of instance {} from {} in {} ({} update interval)", instance.getUuid(), this.strategy, this.zone, TitanTime.UPDATE_INTERVAL);
    }

    @Override
    public void unbind() {
        Task running = this.task;
        if (running != null) {
            running.cancel();
        }
        this.task = null;
        this.instance = null;
        this.lastWrittenSecond = Long.MIN_VALUE;
    }

    @Override
    public boolean update() {
        Instance target = this.instance;
        if (target == null) {
            return false;
        }
        long second = this.clock.instant().getEpochSecond();
        if (second == this.lastWrittenSecond) {
            return false;
        }
        this.lastWrittenSecond = second;
        target.setTime(currentTicks());
        return true;
    }

    @Override
    public @Nullable Instance boundInstance() {
        return this.instance;
    }

    /**
     * Switches off the day cycle the instance runs by itself.
     *
     * <p>Minestom 26.1 replaced {@code Instance#setTimeRate(int)} with a per-dimension
     * {@link net.minestom.server.instance.Clock}, and a dimension may have none at all — that is
     * what
     * {@link Instance#defaultClock()} returning {@code null} means. Such an instance has no day
     * time
     * to drive, so the service says so once instead of failing later with a silent no-op on every
     * write.
     *
     * @param instance the instance to freeze
     */
    private static void freezeOwnCycle(Instance instance) {
        net.minestom.server.instance.Clock worldClock = instance.defaultClock();
        if (worldClock == null) {
            LOGGER.warn("Instance {} (dimension {}) has no default clock; its day time cannot be driven", instance.getUuid(), instance.getDimensionName());
            return;
        }
        worldClock.rate(FROZEN_RATE);
    }
}
