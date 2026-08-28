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

/**
 * Runs the resource pack timeout guard after a delay.
 *
 * <p>Deliberately not Minestom's scheduler: the guard has to fire while a configuration thread
 * is parked inside {@code ConnectionManager.doConfiguration()}, and it must be replaceable in
 * tests so the timeout path can be exercised without waiting for real time to pass.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
@FunctionalInterface
public interface PackTimeoutScheduler {

    /**
     * Runs a task once, after the given delay.
     *
     * @param task  the guard to run
     * @param delay how long to wait before running it
     */
    void schedule(Runnable task, Duration delay);

    /**
     * A scheduler that runs the task immediately on the calling thread, as though the client had
     * already fallen silent. Only meaningful in tests.
     *
     * @return an immediate scheduler
     */
    static PackTimeoutScheduler immediate() {
        return (task, delay) -> task.run();
    }

    /**
     * A scheduler that never runs the task, as though every client answered in time. Useful in
     * tests that are about delivery rather than about the guard.
     *
     * @return a scheduler that drops every guard
     */
    static PackTimeoutScheduler never() {
        return (task, delay) -> {
        };
    }
}
