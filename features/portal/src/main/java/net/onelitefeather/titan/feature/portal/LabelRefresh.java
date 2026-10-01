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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.minestom.server.ServerFlag;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.jetbrains.annotations.Nullable;
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
    private final Set<String> warned = ConcurrentHashMap.newKeySet();
    private boolean stopped;
    private Task task;

    LabelRefresh(Scheduler scheduler, Executor executor, LabelReadings readings, List<Entry> entries) {
        this.scheduler = scheduler;
        this.executor = executor;
        this.readings = readings;
        this.entries = List.copyOf(entries);
    }

    void start(int periodSeconds) {
        // Tick-based on purpose: Minestom's time-based schedules run on a wall-clock timer outside the tick.
        TaskSchedule period = TaskSchedule.tick(Math.multiplyExact(periodSeconds, ServerFlag.SERVER_TICKS_PER_SECOND));
        this.task = this.scheduler.scheduleTask(this::refresh, TaskSchedule.nextTick(), period);
    }

    /** After this returns no label is applied any more, so the displays may be removed. */
    synchronized void stop() {
        this.stopped = true;
        if (this.task != null) {
            this.task.cancel();
        }
    }

    private void refresh() {
        if (!this.reading.compareAndSet(false, true)) {
            LOGGER.debug("Skipping a portal label refresh, the previous read is still running");
            return;
        }
        try {
            this.executor.execute(this::read);
        } catch (RuntimeException e) {
            this.reading.set(false);
            failed("execute", "Starting the portal label read failed, retrying next period", e);
        }
    }

    private void read() {
        try {
            List<Component> rendered = new ArrayList<>(this.entries.size());
            for (Entry entry : this.entries) {
                rendered.add(renderOrNull(entry));
            }
            this.scheduler.scheduleNextTick(() -> apply(rendered));
        } catch (RuntimeException e) {
            failed("batch", "Reading the portal label counts failed, keeping the displayed texts", e);
        } finally {
            this.reading.set(false);
        }
    }

    // One failing entry keeps its old text and leaves the others alone.
    private @Nullable Component renderOrNull(Entry entry) {
        try {
            return LabelRenderer.render(entry.portal(), entry.label(), this.readings.read(entry.portal()));
        } catch (RuntimeException e) {
            failed("entry:" + entry.portal().id(), "Reading the label of portal '" + entry.portal().id() + "' failed, keeping its displayed text", e);
            return null;
        }
    }

    // Only the first failure of a kind is a warning; a provider that stays down must not flood the log.
    private void failed(String key, String message, RuntimeException e) {
        if (this.warned.add(key)) {
            LOGGER.warn("{}: {}", message, e.getMessage());
        }
        LOGGER.debug(message, e);
    }

    // Synchronized with stop(): the displays are removed only once no apply can still touch them.
    private synchronized void apply(List<Component> rendered) {
        if (this.stopped) {
            return;
        }
        for (int i = 0; i < rendered.size(); i++) {
            Entry entry = this.entries.get(i);
            Component text = rendered.get(i);
            if (text != null && entry.display().update(text)) {
                LOGGER.debug("Portal label of '{}' changed", entry.portal().id());
            }
        }
    }
}
