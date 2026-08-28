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
package net.onelitefeather.titan.common.resourcepack;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The production {@link PackTimeoutScheduler}: a single daemon thread that fires the pending
 * timeout guards.
 *
 * <p>One thread is enough because a guard only reads a future and completes it. The thread is a
 * daemon so a lingering timeout can never keep the JVM alive after shutdown.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class DaemonPackTimeoutScheduler implements PackTimeoutScheduler, AutoCloseable {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> Thread.ofPlatform().name("titan-resourcepack-timeout").daemon(true).unstarted(runnable));

    @Override
    public void schedule(Runnable task, Duration delay) {
        this.executor.schedule(task, Math.max(0L, delay.toMillis()), TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the scheduler. Pending guards are dropped; the configuration threads they would have
     * released are gone by then anyway.
     */
    @Override
    public void close() {
        this.executor.shutdownNow();
    }
}
