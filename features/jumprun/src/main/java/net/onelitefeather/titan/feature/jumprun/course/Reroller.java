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
package net.onelitefeather.titan.feature.jumprun.course;

import java.util.function.IntSupplier;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

/**
 * The beat of a Rainbow or Ultra run: counts the ticks the runner stands on the current block and
 * rerolls when the interval is full. A tick without standing, and every landing on another block,
 * starts the count again, so a jump never leaves half an interval behind. Lives on the scheduler
 * of the runner and ends with the run.
 *
 * <p>Every step takes the lock of the run and drops out once stopped, so a tick that outlives its
 * run is harmless.
 */
public final class Reroller {

    /** What {@code standingOn} answers while the runner does not stand on the current block. */
    public static final int NOT_STANDING = -1;

    private final Object lock;
    private final int interval;
    private final IntSupplier standingOn;
    private final Runnable reroll;
    private Task task;
    private int standingTicks;
    private int lastBlock = NOT_STANDING;
    private boolean stopped;

    /**
     * @param lock       the lock of the run the callbacks belong to
     * @param interval   standing ticks between two rerolls, above 0
     * @param standingOn the index of the current block while the runner stands on it, else
     *                   {@link #NOT_STANDING}
     * @param reroll     what to do when the interval is full; it runs under {@code lock}
     */
    public Reroller(Object lock, int interval, IntSupplier standingOn, Runnable reroll) {
        this.lock = lock;
        this.interval = interval;
        this.standingOn = standingOn;
        this.reroll = reroll;
    }

    /** Beats once per tick on the scheduler of the player until {@link #stop()}. */
    public void start(Player player) {
        task = player.scheduler().buildTask(this::tick).repeat(TaskSchedule.tick(1)).schedule();
    }

    /** One tick of the beat; the scheduler calls it, tests may too. */
    void tick() {
        synchronized (lock) {
            if (stopped) {
                return;
            }
            int block = standingOn.getAsInt();
            if (block != lastBlock) {
                lastBlock = block;
                standingTicks = 0;
            }
            if (block != NOT_STANDING && ++standingTicks >= interval) {
                standingTicks = 0;
                reroll.run();
            }
        }
    }

    /** No tick does anything after this. */
    public void stop() {
        synchronized (lock) {
            stopped = true;
        }
        if (task != null) {
            task.cancel();
        }
    }
}
