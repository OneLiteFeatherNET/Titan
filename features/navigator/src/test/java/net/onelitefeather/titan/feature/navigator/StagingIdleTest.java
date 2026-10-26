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
package net.onelitefeather.titan.feature.navigator;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakeOnDemandServices;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The idle stop of staging services, against fakes and an explicit clock; no tick and no sleep. */
class StagingIdleTest {

    private static final Destination STAGING = Destination.SLENDER_STAGING;
    private static final String TASK = STAGING.task();
    private static final String SERVICE = "Slender-Staging-1";
    private static final Duration TEN_MINUTES = Duration.ofSeconds(600);

    private final FakeOnDemandServices services = new FakeOnDemandServices();
    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-26T12:00:00Z"));
    private final StagingIdle idle = new StagingIdle(this.services, this.clock, TEN_MINUTES);

    @DisplayName("A staging service that stood empty for less than 600 seconds is not stopped")
    @Test
    void notStoppedBeforeTheIdleLimit() {
        this.services.serve(TASK, running(0));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(599));

        refresh(this.idle);

        Assertions.assertTrue(this.services.stopped().isEmpty(), "599 seconds of idleness must not stop the service");
    }

    @DisplayName("A staging service that stood empty for 600 seconds is stopped")
    @Test
    void stoppedAtTheIdleLimit() {
        this.services.serve(TASK, running(0));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(599));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(1));

        refresh(this.idle);

        Assertions.assertEquals(List.of(SERVICE), this.services.stopped(), "600 seconds of idleness stop the service");
    }

    @DisplayName("A player on the service restarts the idle count")
    @Test
    void playerResetsTheIdleTimer() {
        this.services.serve(TASK, running(0));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(500));
        this.services.serve(TASK, running(1));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(100));
        this.services.serve(TASK, running(0));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(599));

        refresh(this.idle);
        Assertions.assertTrue(this.services.stopped().isEmpty(), "599 seconds after the player left must not stop the service");

        this.clock.advance(Duration.ofSeconds(1));
        refresh(this.idle);
        Assertions.assertEquals(List.of(SERVICE), this.services.stopped(), "600 seconds after the player left stop the service");
    }

    @DisplayName("A failed read stops nothing and the idle time does not run while reads fail")
    @Test
    void readErrorFreezesTheIdleTimer() {
        this.services.serve(TASK, running(0));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(300));
        refresh(this.idle);

        this.services.failReads(true);
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(400));
        refresh(this.idle);
        this.services.failReads(false);
        refresh(this.idle);
        Assertions.assertTrue(this.services.stopped().isEmpty(), "a failed read must stop nothing");

        this.clock.advance(Duration.ofSeconds(299));
        refresh(this.idle);
        Assertions.assertTrue(this.services.stopped().isEmpty(), "the 400 failing seconds must not count as idle");

        this.clock.advance(Duration.ofSeconds(1));
        refresh(this.idle);
        Assertions.assertEquals(List.of(SERVICE), this.services.stopped(), "the readable idle time counts up to the limit");
    }

    @DisplayName("A live task is never stopped, even when empty for longer than the limit")
    @Test
    void liveTasksAreNeverStopped() {
        Destination live = Destination.SLENDER;
        this.services.serve(live.task(), new ServiceState("Slender-1", Phase.RUNNING, 0, 10));
        Map<Destination, Optional<List<ServiceState>>> reads = new EnumMap<>(Destination.class);
        reads.put(live, Optional.of(List.of(new ServiceState("Slender-1", Phase.RUNNING, 0, 10))));
        this.idle.refresh(reads);
        this.clock.advance(Duration.ofSeconds(3600));

        this.idle.refresh(reads);

        Assertions.assertTrue(this.services.stopped().isEmpty(), "only staging tasks may be stopped");
    }

    @DisplayName("A service that is still starting is never counted as idle")
    @Test
    void startingServiceIsNeverStopped() {
        this.services.serve(TASK, new ServiceState(SERVICE, Phase.STARTING, 0, 0));
        refresh(this.idle);
        this.clock.advance(Duration.ofSeconds(3600));

        refresh(this.idle);

        Assertions.assertTrue(this.services.stopped().isEmpty(), "a service that has not started yet is not idle");
    }

    @DisplayName("A fresh read right before the stop sees a player who joined, and nothing is stopped")
    @Test
    void freshReadBeforeStopSeesAPlayer() {
        this.services.serve(TASK, running(0));
        JoinOnNextRead provider = new JoinOnNextRead(this.services);
        StagingIdle checked = new StagingIdle(provider, this.clock, TEN_MINUTES);
        Map<Destination, Optional<List<ServiceState>>> stale = Map.of(STAGING, Optional.of(List.of(running(0))));
        checked.refresh(stale);
        this.clock.advance(TEN_MINUTES);
        provider.joinOnNextRead();

        checked.refresh(stale);

        Assertions.assertEquals(1, provider.reads, "the stop is preceded by exactly one fresh read");
        Assertions.assertTrue(this.services.stopped().isEmpty(), "a player found by the fresh read prevents the stop");
    }

    @DisplayName("Two lobbies that both reach the limit stop the service once and the second stop does not fail")
    @Test
    void secondStopDoesNotFail() {
        this.services.serve(TASK, running(0));
        StagingIdle other = new StagingIdle(this.services, this.clock, TEN_MINUTES);
        refresh(this.idle);
        refresh(other);
        this.clock.advance(TEN_MINUTES);

        refresh(this.idle);
        refresh(other);

        Assertions.assertEquals(List.of(SERVICE), this.services.stopped(), "the service is stopped once");
    }

    private static ServiceState running(int online) {
        return new ServiceState(SERVICE, Phase.RUNNING, online, 10);
    }

    /** Reads the staging task as a refresh cycle does, then runs one idle pass over it. */
    private void refresh(StagingIdle target) {
        target.refresh(Map.of(STAGING, StagingStarts.read(this.services, STAGING)));
    }

    /** Lets a player join between the cycle's read and the fresh read before a stop. */
    private static final class JoinOnNextRead implements OnDemandServices {

        private final FakeOnDemandServices fake;
        private boolean joinNext;
        private int reads;

        JoinOnNextRead(FakeOnDemandServices fake) {
            this.fake = fake;
        }

        void joinOnNextRead() {
            this.joinNext = true;
        }

        @Override
        public Optional<List<ServiceState>> services(String task) {
            this.reads++;
            if (this.joinNext) {
                this.joinNext = false;
                this.fake.serve(task, running(1));
            }
            return this.fake.services(task);
        }

        @Override
        public boolean start(String task) {
            return this.fake.start(task);
        }

        @Override
        public boolean stop(String serviceName) {
            return this.fake.stop(serviceName);
        }
    }
}
