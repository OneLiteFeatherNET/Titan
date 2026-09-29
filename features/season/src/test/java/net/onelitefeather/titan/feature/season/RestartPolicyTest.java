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

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestartPolicyTest {

    private final RestartPolicy policy = new RestartPolicy();

    @DisplayName("The same world needs no restart, even with an empty lobby")
    @Test
    void sameWorldNeedsNoRestart() {
        Assertions.assertEquals(RestartPolicy.Decision.NONE, this.policy.decide(Optional.of("winter"), Optional.of("winter"), 0));
    }

    @DisplayName("The default world on both sides needs no restart")
    @Test
    void defaultWorldOnBothSidesNeedsNoRestart() {
        Assertions.assertEquals(RestartPolicy.Decision.NONE, this.policy.decide(Optional.empty(), Optional.empty(), 0));
    }

    @DisplayName("A different world with players online is pending")
    @Test
    void differentWorldWithPlayersIsPending() {
        Assertions.assertEquals(RestartPolicy.Decision.PENDING, this.policy.decide(Optional.empty(), Optional.of("winter"), 1));
    }

    @DisplayName("A different world with an empty lobby stops")
    @Test
    void differentWorldWithEmptyLobbyStops() {
        Assertions.assertEquals(RestartPolicy.Decision.STOP, this.policy.decide(Optional.empty(), Optional.of("winter"), 0));
    }

    @DisplayName("Leaving a season for the default world also restarts")
    @Test
    void leavingASeasonRestartsIntoTheDefaultWorld() {
        Assertions.assertEquals(RestartPolicy.Decision.STOP, this.policy.decide(Optional.of("winter"), Optional.empty(), 0));
    }
}
