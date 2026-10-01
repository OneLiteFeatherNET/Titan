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
package net.onelitefeather.titan.feature.portal;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the counts of all labels off the tick, then renders and applies them on the scheduler.
 *
 * <p>The read may block (a provider can call over the network), so it runs on the executor; the
 * entity metadata is only touched from a scheduler task. A period that finds the previous read
 * still running is skipped, so a slow provider never piles up work.
 */
final class LabelRefresh {

    private static final Logger LOGGER = LoggerFactory.getLogger(LabelRefresh.class);

    /** A label with the display that shows it. */
    record Entry(Portal portal, PortalLabel label, LabelDisplay display) {
    }

    private final Scheduler scheduler;
    private final Executor executor;
    private final LabelReadings readings;
    private final List<Entry> entries;
    private final AtomicBoolean reading = new AtomicBoolean();
    private volatile boolean stopped;
    private Task task;

    LabelRefresh(Scheduler scheduler, Executor executor, LabelReadings readings, List<Entry> entries) {
        this.scheduler = scheduler;
        this.executor = executor;
        this.readings = readings;
        this.entries = List.copyOf(entries);
    }

    void start(int periodSeconds) {
        TaskSchedule period = TaskSchedule.tick(periodSeconds * 20);
        this.task = this.scheduler.scheduleTask(this::refresh, TaskSchedule.nextTick(), period);
    }

    void stop() {
        this.stopped = true;
        this.task.cancel();
    }

    private void refresh() {
        if (!this.reading.compareAndSet(false, true)) {
            LOGGER.debug("Skipping a portal label refresh, the previous read is still running");
            return;
        }
        this.executor.execute(this::read);
    }

    private void read() {
        try {
            List<Component> rendered = this.entries.stream().map(entry -> LabelRenderer.render(entry.portal(), entry.label(), this.readings.read(entry.portal(), entry.label()))).toList();
            this.scheduler.scheduleNextTick(() -> apply(rendered));
        } catch (RuntimeException e) {
            LOGGER.warn("Reading the portal label counts failed, keeping the displayed texts", e);
        } finally {
            this.reading.set(false);
        }
    }

    private void apply(List<Component> rendered) {
        if (this.stopped) {
            return;
        }
        for (int i = 0; i < rendered.size(); i++) {
            Entry entry = this.entries.get(i);
            if (entry.display().update(rendered.get(i))) {
                LOGGER.debug("Portal label of '{}' changed", entry.portal().id());
            }
        }
    }
}
