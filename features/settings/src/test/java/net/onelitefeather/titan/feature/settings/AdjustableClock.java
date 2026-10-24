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
package net.onelitefeather.titan.feature.settings;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock the test moves forward by hand, so debounce and retry timing never waits. */
final class AdjustableClock extends Clock {

    private final ZoneId zone;
    private Instant now;

    AdjustableClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    void advance(Duration duration) {
        this.now = this.now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return this.zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new AdjustableClock(this.now, zone);
    }

    @Override
    public Instant instant() {
        return this.now;
    }
}
