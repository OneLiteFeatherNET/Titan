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
package net.onelitefeather.titan.feature.throwing;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrowPortalGuardTest {

    private static final Instant LAUNCH = Instant.parse("2026-10-24T12:00:00Z");

    @DisplayName("A thrown player may not pass a portal during the flight")
    @Test
    void flightBlocksPortalTransfer() {
        FlightProtection flights = new FlightProtection();
        UUID target = UUID.randomUUID();
        flights.launch(target, LAUNCH);

        Assertions.assertFalse(flights.mayTransfer(target, LAUNCH.plusSeconds(1)), "a player in the air must not be handed to another server");
    }

    @DisplayName("A flight ends after five seconds even without a landing")
    @Test
    void flightEndsAfterFiveSecondsWithoutLanding() {
        FlightProtection flights = new FlightProtection();
        UUID target = UUID.randomUUID();
        flights.launch(target, LAUNCH);

        Assertions.assertTrue(flights.inFlight(target, LAUNCH.plusSeconds(4)), "four seconds into the flight it is still running");
        Assertions.assertFalse(flights.inFlight(target, LAUNCH.plusSeconds(5)), "five seconds into the flight it must end, landing or not");
    }

    @DisplayName("After landing the portal stays blocked for two seconds")
    @Test
    void landingKeepsPortalBlockedForTwoSeconds() {
        FlightProtection flights = new FlightProtection();
        UUID target = UUID.randomUUID();
        flights.launch(target, LAUNCH);
        flights.observeGround(target, false, LAUNCH.plusMillis(100));
        flights.observeGround(target, true, LAUNCH.plusSeconds(1));

        Assertions.assertFalse(flights.mayTransfer(target, LAUNCH.plusSeconds(2)), "one second after landing the portal must still be blocked");
        Assertions.assertTrue(flights.mayTransfer(target, LAUNCH.plusSeconds(3)), "two seconds after landing the portal logic applies again");
    }

    @DisplayName("A player who was never thrown may pass a portal")
    @Test
    void unthrownPlayerMayTransfer() {
        Assertions.assertTrue(new FlightProtection().mayTransfer(UUID.randomUUID(), LAUNCH), "the guard must not hold back players that were never thrown");
    }

    @DisplayName("Standing on the ground right after the launch does not count as landing")
    @Test
    void groundAtLaunchIsNotALanding() {
        FlightProtection flights = new FlightProtection();
        UUID target = UUID.randomUUID();
        flights.launch(target, LAUNCH);
        flights.observeGround(target, true, LAUNCH.plusMillis(50));

        Assertions.assertTrue(flights.inFlight(target, LAUNCH.plusSeconds(1)), "the player is still in the flight until they have left the ground");
    }

    @DisplayName("Forgetting a player removes their protection")
    @Test
    void forgetRemovesProtection() {
        FlightProtection flights = new FlightProtection();
        UUID target = UUID.randomUUID();
        flights.launch(target, LAUNCH);
        flights.forget(target);

        Assertions.assertTrue(flights.mayTransfer(target, LAUNCH.plusSeconds(1)), "a forgotten player is no longer protected");
        Assertions.assertEquals(Duration.ofSeconds(5), FlightProtection.MAX_FLIGHT, "the flight is capped at five seconds");
    }
}
