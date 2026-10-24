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
package net.onelitefeather.titan.platform.otis;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CircuitBreakerTest {

    private static final Instant START = Instant.parse("2026-10-24T12:00:00Z");
    private static final Duration OPEN_FOR = Duration.ofSeconds(30);

    private final MovableClock clock = new MovableClock(START);
    private final List<CircuitBreaker.State> transitions = new ArrayList<>();
    private final CircuitBreaker breaker = new CircuitBreaker(3, OPEN_FOR, this.clock, this.transitions::add);

    @DisplayName("Three failures in a row open the breaker, and calls are refused")
    @Test
    void threeFailuresOpen() {
        failThreeTimes();
        Assertions.assertFalse(this.breaker.tryAcquire(), "an open breaker must refuse calls");
        Assertions.assertEquals(List.of(CircuitBreaker.State.OPEN), this.transitions, "opening must be reported once");
    }

    @DisplayName("Two failures in a row keep the breaker closed")
    @Test
    void twoFailuresStayClosed() {
        this.breaker.recordFailure();
        this.breaker.recordFailure();
        Assertions.assertTrue(this.breaker.tryAcquire(), "two failures are below the threshold of three");
    }

    @DisplayName("A success in between restarts the count of failures")
    @Test
    void successResetsTheCount() {
        this.breaker.recordFailure();
        this.breaker.recordFailure();
        this.breaker.recordSuccess();
        this.breaker.recordFailure();
        this.breaker.recordFailure();
        Assertions.assertTrue(this.breaker.tryAcquire(), "failures that are not in a row must not open the breaker");
    }

    @DisplayName("After the wait exactly one probe is let through")
    @Test
    void afterTheWaitOneProbeIsAllowed() {
        failThreeTimes();
        this.clock.advance(OPEN_FOR);
        Assertions.assertTrue(this.breaker.tryAcquire(), "the first call after the wait is the probe");
        Assertions.assertFalse(this.breaker.tryAcquire(), "a second call while the probe is out must be refused");
    }

    @DisplayName("A successful probe closes the breaker again")
    @Test
    void successfulProbeCloses() {
        failThreeTimes();
        this.clock.advance(OPEN_FOR);
        this.breaker.tryAcquire();
        this.breaker.recordSuccess();
        Assertions.assertTrue(this.breaker.tryAcquire(), "after a successful probe calls must go through again");
        Assertions.assertEquals(List.of(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED), this.transitions, "every transition must be reported in order");
    }

    @DisplayName("A failed probe opens the breaker for another wait")
    @Test
    void failedProbeReopens() {
        failThreeTimes();
        this.clock.advance(OPEN_FOR);
        this.breaker.tryAcquire();
        this.breaker.recordFailure();
        Assertions.assertFalse(this.breaker.tryAcquire(), "a failed probe must open the breaker again");
        this.clock.advance(Duration.ofSeconds(29));
        Assertions.assertFalse(this.breaker.tryAcquire(), "the new wait must run its full length");
    }

    private void failThreeTimes() {
        this.breaker.recordFailure();
        this.breaker.recordFailure();
        this.breaker.recordFailure();
    }

    /** A clock the test moves forward by hand. */
    private static final class MovableClock extends java.time.Clock {

        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }
}
