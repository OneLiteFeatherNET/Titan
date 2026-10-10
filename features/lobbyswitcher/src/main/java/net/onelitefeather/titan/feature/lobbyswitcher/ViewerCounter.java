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

/**
 * Counts the players looking at the lobby list and runs the periodic read only while someone
 * does: the first viewer starts it, the last one stops it.
 */
final class ViewerCounter {

    // Minestom's TICKS_PER_SECOND is marked for removal; Titan runs the default rate.
    static final int TICKS_PER_SECOND = 20;

    private final ReadScheduling scheduling;
    private final int periodTicks;
    private int viewers;
    private ReadScheduling.Period period;

    ViewerCounter(ReadScheduling scheduling, int refreshSeconds) {
        this.scheduling = scheduling;
        this.periodTicks = refreshSeconds * TICKS_PER_SECOND;
    }

    /** A viewer opened the list: reads once now and, for the first viewer, starts the period. */
    synchronized void opened() {
        this.viewers++;
        if (this.viewers == 1) {
            this.period = this.scheduling.start(this.periodTicks);
        }
        this.scheduling.requestRead();
    }

    /**
     * A viewer closed the list; the last one stops the period. A close without a viewer is ignored.
     */
    synchronized void closed() {
        if (this.viewers == 0) {
            return;
        }
        this.viewers--;
        if (this.viewers == 0) {
            this.period.stop();
            this.period = null;
        }
    }

    /** Ends the period whatever the viewer count, for shutdown; later closes are ignored. */
    synchronized void stop() {
        this.viewers = 0;
        if (this.period != null) {
            this.period.stop();
            this.period = null;
        }
    }
}
