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
package net.onelitefeather.titan.feature.tickle;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Every test builds its own limiter and clock (F.I.R.S.T. - Independent, Repeatable). */
class TicklePairLimiterTest {

    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");

    private final AdjustableClock clock = new AdjustableClock(START, ZoneOffset.UTC);
    private final TicklePairLimiter limiter = new TicklePairLimiter(this.clock);
    private final UUID alex = UUID.randomUUID();
    private final UUID sam = UUID.randomUUID();

    @DisplayName("The first tickle of a pair may play its sound")
    @Test
    void firstTickleOfPairMayPlay() {
        Assertions.assertTrue(this.limiter.tryAcquire(this.alex, this.sam), "a pair that has not tickled yet must be allowed");
    }

    @DisplayName("A second tickle of the same pair within 2 seconds is refused")
    @Test
    void secondTickleWithinIntervalIsRefused() {
        this.limiter.tryAcquire(this.alex, this.sam);
        this.clock.advance(Duration.ofSeconds(1));

        Assertions.assertFalse(this.limiter.tryAcquire(this.alex, this.sam), "a tickle sound inside the 2 s window must be refused");
    }

    @DisplayName("A tickle of the same pair exactly 2 seconds later may play again")
    @Test
    void tickleAfterIntervalMayPlayAgain() {
        this.limiter.tryAcquire(this.alex, this.sam);
        this.clock.advance(TicklePairLimiter.INTERVAL);

        Assertions.assertTrue(this.limiter.tryAcquire(this.alex, this.sam), "the window ends exactly after the interval");
    }

    @DisplayName("A refused tickle does not restart the window")
    @Test
    void refusedTickleDoesNotExtendWindow() {
        this.limiter.tryAcquire(this.alex, this.sam);
        this.clock.advance(Duration.ofSeconds(1));
        this.limiter.tryAcquire(this.alex, this.sam);
        this.clock.advance(Duration.ofSeconds(1));

        Assertions.assertTrue(this.limiter.tryAcquire(this.alex, this.sam), "the window is measured from the last sound, not the last attempt");
    }

    @DisplayName("Another target does not share the window")
    @Test
    void otherPairIsNotAffected() {
        this.limiter.tryAcquire(this.alex, this.sam);

        Assertions.assertTrue(this.limiter.tryAcquire(this.alex, UUID.randomUUID()), "a different target is a different pair");
    }

    @DisplayName("The pair is ordered: the reverse tickle has its own window")
    @Test
    void pairIsOrdered() {
        this.limiter.tryAcquire(this.alex, this.sam);

        Assertions.assertTrue(this.limiter.tryAcquire(this.sam, this.alex), "sam tickling alex is a different pair than alex tickling sam");
    }
}
