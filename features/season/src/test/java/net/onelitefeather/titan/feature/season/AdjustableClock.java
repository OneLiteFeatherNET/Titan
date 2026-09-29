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
package net.onelitefeather.titan.feature.season;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A test-only {@link Clock} whose {@link #instant()} is set by the test, so a test can drive
 * {@link SeasonModule} without wall-clock time or {@link Thread#sleep(long)}.
 */
final class AdjustableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    AdjustableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    void advance(Duration duration) {
        this.instant = this.instant.plus(duration);
    }

    void set(Instant instant) {
        this.instant = instant;
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
        return this.instant;
    }
}
