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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Stops calls to Otis after repeated failures, so a dead Otis costs the lobby no waiting. Closed,
 * it lets every call through; after {@code threshold} failures in a row it opens and refuses calls
 * for {@code openFor}. Then it lets exactly one probe through: a success closes it again, a failure
 * opens it for another {@code openFor}.
 *
 * <p>The time comes from the {@link Clock}, so a test moves it forward without waiting. Not thread
 * safe on its own; every method is synchronized.
 */
final class CircuitBreaker {

    enum State {
        CLOSED("closed"), OPEN("open"), HALF_OPEN("half_open");

        private final String telemetryName;

        State(String telemetryName) {
            this.telemetryName = telemetryName;
        }

        /** The state's name in telemetry. */
        String telemetryName() {
            return this.telemetryName;
        }
    }

    private final int threshold;
    private final Duration openFor;
    private final Clock clock;
    private final Consumer<State> onTransition;
    private State state = State.CLOSED;
    private int failures;
    private Instant openedAt;
    private boolean probeInFlight;

    CircuitBreaker(int threshold, Duration openFor, Clock clock, Consumer<State> onTransition) {
        this.threshold = threshold;
        this.openFor = Objects.requireNonNull(openFor, "openFor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.onTransition = Objects.requireNonNull(onTransition, "onTransition must not be null");
    }

    /**
     * @return {@code true} if a call may be made now; a call allowed while half-open is the probe,
     *         and the caller must report its outcome
     */
    synchronized boolean tryAcquire() {
        switch (this.state) {
            case CLOSED:
                return true;
            case OPEN:
                if (this.clock.instant().isBefore(this.openedAt.plus(this.openFor))) {
                    return false;
                }
                transition(State.HALF_OPEN);
                this.probeInFlight = true;
                return true;
            case HALF_OPEN:
                if (this.probeInFlight) {
                    return false;
                }
                this.probeInFlight = true;
                return true;
            default:
                throw new IllegalStateException("Unknown state " + this.state);
        }
    }

    synchronized void recordSuccess() {
        if (this.state == State.OPEN) {
            // A call started before the breaker opened; it says nothing about the probe.
            return;
        }
        this.probeInFlight = false;
        this.failures = 0;
        transition(State.CLOSED);
    }

    synchronized void recordFailure() {
        if (this.state == State.OPEN) {
            return;
        }
        this.probeInFlight = false;
        if (this.state == State.HALF_OPEN) {
            open();
            return;
        }
        this.failures++;
        if (this.failures >= this.threshold) {
            open();
        }
    }

    private void open() {
        this.openedAt = this.clock.instant();
        this.failures = 0;
        transition(State.OPEN);
    }

    private void transition(State next) {
        if (this.state != next) {
            this.state = next;
            this.onTransition.accept(next);
        }
    }
}
