/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the reads of the lobby list: the period is a tick-based scheduler task, the read itself
 * happens on the executor because a provider may block on the network, and the result is applied
 * on the next tick, the only place the inventory is touched. A read that finds the previous one
 * still running is skipped, so a slow provider never piles up work.
 */
final class SchedulerReadScheduling implements ReadScheduling {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerReadScheduling.class);

    private final Scheduler scheduler;
    private final Executor executor;
    private final Supplier<SwitcherReading.Outcome> read;
    private final Consumer<SwitcherReading.Outcome> apply;
    private final AtomicBoolean reading = new AtomicBoolean();

    SchedulerReadScheduling(Scheduler scheduler, Executor executor, Supplier<SwitcherReading.Outcome> read, Consumer<SwitcherReading.Outcome> apply) {
        this.scheduler = scheduler;
        this.executor = executor;
        this.read = read;
        this.apply = apply;
    }

    @Override
    public Period start(int periodTicks) {
        // Tick-based on purpose: Minestom's time-based schedules run on a wall-clock timer outside
        // the tick. The first period read is one period away; opening already read.
        TaskSchedule period = TaskSchedule.tick(periodTicks);
        return this.scheduler.scheduleTask(this::requestRead, period, period)::cancel;
    }

    @Override
    public void requestRead() {
        if (!this.reading.compareAndSet(false, true)) {
            LOGGER.debug("Skipping a lobby list read, the previous one is still running");
            return;
        }
        try {
            this.executor.execute(this::readOffTick);
        } catch (RuntimeException e) {
            this.reading.set(false);
            LOGGER.warn("Starting the lobby list read failed: {}", e.toString());
        }
    }

    private void readOffTick() {
        try {
            SwitcherReading.Outcome outcome = this.read.get();
            this.scheduler.scheduleNextTick(() -> this.apply.accept(outcome));
        } finally {
            this.reading.set(false);
        }
    }
}
