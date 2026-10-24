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
package net.onelitefeather.titan.core.testfixtures;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A test-only {@link Clock} that only moves when a test says so, so no test depends on wall-clock
 * time or {@link Thread#sleep(long)}. It also counts how often it was read.
 */
public final class AdjustableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;
    private int reads;

    public AdjustableClock(Instant instant) {
        this(instant, ZoneOffset.UTC);
    }

    public AdjustableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    /**
     * Moves this clock's current time forward by {@code duration}.
     *
     * @param duration the amount of time to advance by
     */
    public void advance(Duration duration) {
        this.instant = this.instant.plus(duration);
    }

    /** Sets this clock's current time to {@code instant}. */
    public void set(Instant instant) {
        this.instant = instant;
    }

    /** How often {@link #instant()} was read. */
    public int reads() {
        return this.reads;
    }

    @Override
    public ZoneId getZone() {
        return this.zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new AdjustableClock(this.instant, zone);
    }

    @Override
    public Instant instant() {
        this.reads++;
        return this.instant;
    }
}
