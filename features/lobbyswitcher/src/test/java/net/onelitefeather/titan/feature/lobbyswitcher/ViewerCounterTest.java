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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ViewerCounterTest {

    /** Records what the counter asks of the scheduler. */
    private static final class FakeScheduling implements ReadScheduling {
        final List<Integer> startedPeriods = new ArrayList<>();
        int stopped;
        int readRequests;

        @Override
        public Period start(int periodTicks) {
            this.startedPeriods.add(periodTicks);
            return () -> this.stopped++;
        }

        @Override
        public void requestRead() {
            this.readRequests++;
        }
    }

    private final FakeScheduling scheduling = new FakeScheduling();
    private final ViewerCounter counter = new ViewerCounter(this.scheduling, 5);

    @DisplayName("The first viewer starts the period with refreshSeconds times 20 ticks")
    @Test
    void firstViewerStartsPeriod() {
        this.counter.opened();

        Assertions.assertEquals(List.of(100), this.scheduling.startedPeriods);
    }

    @DisplayName("A second viewer does not start a second period")
    @Test
    void secondViewerStartsNothing() {
        this.counter.opened();
        this.counter.opened();

        Assertions.assertEquals(1, this.scheduling.startedPeriods.size());
    }

    @DisplayName("The period keeps running while one viewer is left")
    @Test
    void periodSurvivesWhileViewersRemain() {
        this.counter.opened();
        this.counter.opened();
        this.counter.closed();

        Assertions.assertEquals(0, this.scheduling.stopped);
    }

    @DisplayName("The last viewer stops the period")
    @Test
    void lastViewerStopsPeriod() {
        this.counter.opened();
        this.counter.opened();
        this.counter.closed();
        this.counter.closed();

        Assertions.assertEquals(1, this.scheduling.stopped);
    }

    @DisplayName("Every opening requests an immediate read")
    @Test
    void openingRequestsRead() {
        this.counter.opened();
        this.counter.opened();

        Assertions.assertEquals(2, this.scheduling.readRequests);
    }

    @DisplayName("Closing requests no read")
    @Test
    void closingRequestsNoRead() {
        this.counter.opened();
        this.counter.closed();

        Assertions.assertEquals(1, this.scheduling.readRequests, "only the opening read");
    }

    @DisplayName("Without a viewer no period runs")
    @Test
    void noViewerNoPeriod() {
        Assertions.assertEquals(List.of(), this.scheduling.startedPeriods);
        Assertions.assertEquals(0, this.scheduling.readRequests);
    }

    @DisplayName("Closing without a viewer changes nothing")
    @Test
    void closingWithoutViewerIsIgnored() {
        this.counter.closed();
        this.counter.opened();

        Assertions.assertEquals(1, this.scheduling.startedPeriods.size(), "the stray close did not eat the next viewer");
        Assertions.assertEquals(0, this.scheduling.stopped);
    }

    @DisplayName("A viewer after the last one left starts a fresh period")
    @Test
    void reopenStartsAgain() {
        this.counter.opened();
        this.counter.closed();
        this.counter.opened();

        Assertions.assertEquals(2, this.scheduling.startedPeriods.size());
        Assertions.assertEquals(1, this.scheduling.stopped, "the first period stays stopped");
    }
}
