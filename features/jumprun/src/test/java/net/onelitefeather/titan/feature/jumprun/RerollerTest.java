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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The beat of a rerolling run, driven tick by tick without a server. */
class RerollerTest {

    private static final int INTERVAL = 5;

    private final AtomicInteger standingOn = new AtomicInteger(0);
    private final AtomicInteger rerolls = new AtomicInteger();
    private final Reroller reroller = new Reroller(new Object(), INTERVAL, standingOn::get, rerolls::incrementAndGet);

    private void tick(int times) {
        for (int i = 0; i < times; i++) {
            reroller.tick();
        }
    }

    @Test
    void rerollsOnceTheRunnerHasStoodForTheWholeInterval() {
        tick(INTERVAL - 1);
        assertEquals(0, rerolls.get(), "one tick short");

        tick(1);

        assertEquals(1, rerolls.get(), "the interval is full");
    }

    @Test
    void startsCountingAgainAfterEachReroll() {
        tick(INTERVAL * 2 + INTERVAL - 1);

        assertEquals(2, rerolls.get(), "two full intervals, the third one is not full");
    }

    @Test
    void aTickInTheAirStartsTheCountAgain() {
        tick(INTERVAL - 1);
        standingOn.set(Reroller.NOT_STANDING);
        tick(1);
        standingOn.set(0);

        tick(INTERVAL - 1);

        assertEquals(0, rerolls.get(), "the jump took the count with it");
    }

    @Test
    void aLandingOnAnotherBlockStartsTheCountAgainWithoutATickInTheAir() {
        tick(INTERVAL - 1);
        standingOn.set(1);

        tick(INTERVAL - 1);

        assertEquals(0, rerolls.get(), "the count began on the new block");
        tick(1);
        assertEquals(1, rerolls.get(), "a full interval on the new block");
    }

    @Test
    void doesNotCountWhileTheRunnerDoesNotStand() {
        standingOn.set(Reroller.NOT_STANDING);

        tick(INTERVAL * 4);

        assertEquals(0, rerolls.get());
    }

    @Test
    void aStoppedBeatDoesNothingMore() {
        tick(INTERVAL - 1);
        reroller.stop();

        tick(INTERVAL * 3);

        assertEquals(0, rerolls.get(), "nothing fires after the run ended");
    }
}
