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

import java.util.Optional;
import net.onelitefeather.titan.feature.throwing.ThrowRules.Party;
import net.onelitefeather.titan.feature.throwing.ThrowRules.Refusal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrowRulesTest {

    private static final Party FREE = new Party(false, false, false, false, false, false);

    @DisplayName("A pickup without the throw permission is refused")
    @Test
    void withoutPermissionThePickupIsRefused() {
        assertRefusal(Refusal.PERMISSION, ThrowRules.pickup(false, FREE, FREE));
    }

    @DisplayName("An immune target cannot be picked up")
    @Test
    void immuneTargetIsRefused() {
        assertRefusal(Refusal.IMMUNE, ThrowRules.pickup(true, FREE, party(false, false, false, false, true, false)));
    }

    @DisplayName("A target who opted out cannot be picked up")
    @Test
    void optedOutTargetIsRefused() {
        assertRefusal(Refusal.OPT_OUT, ThrowRules.pickup(true, FREE, party(false, false, false, false, false, true)));
    }

    @DisplayName("A target who is busy with a seat, a dance, a jump-and-run or an elytra flight is refused")
    @Test
    void busyTargetIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, FREE, party(true, false, false, false, false, false)));
    }

    @DisplayName("A thrower who is busy cannot throw")
    @Test
    void busyThrowerIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, party(true, false, false, false, false, false), FREE));
    }

    @DisplayName("A target who is already carried cannot be picked up again")
    @Test
    void carriedTargetIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, FREE, party(false, true, false, false, false, false)));
    }

    @DisplayName("A thrower who is carried cannot pick anyone up")
    @Test
    void carriedThrowerIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, party(false, true, false, false, false, false), FREE));
    }

    @DisplayName("A thrower who already carries someone cannot pick up a second one")
    @Test
    void carryingThrowerIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, party(false, false, true, false, false, false), FREE));
    }

    @DisplayName("A target who carries someone cannot be picked up")
    @Test
    void carryingTargetIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, FREE, party(false, false, true, false, false, false)));
    }

    @DisplayName("A target in the air after a throw cannot be picked up")
    @Test
    void targetInFlightIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, FREE, party(false, false, false, true, false, false)));
    }

    @DisplayName("A thrower in the air after a throw cannot throw")
    @Test
    void throwerInFlightIsRefused() {
        assertRefusal(Refusal.STATE, ThrowRules.pickup(true, party(false, false, false, true, false, false), FREE));
    }

    @DisplayName("A permitted pickup of a free target is allowed")
    @Test
    void freeTargetIsAllowed() {
        Assertions.assertTrue(ThrowRules.pickup(true, FREE, FREE).isEmpty(), "nothing blocks a permitted pickup of a free player");
    }

    @DisplayName("Each refusal has the reason name used as telemetry attribute")
    @Test
    void reasonNamesMatchTelemetry() {
        Assertions.assertEquals("permission", Refusal.PERMISSION.reason());
        Assertions.assertEquals("opt_out", Refusal.OPT_OUT.reason());
        Assertions.assertEquals("immune", Refusal.IMMUNE.reason());
        Assertions.assertEquals("cooldown", Refusal.COOLDOWN.reason());
        Assertions.assertEquals("state", Refusal.STATE.reason());
    }

    private static Party party(boolean busy, boolean carried, boolean carrying, boolean inFlight, boolean immune, boolean optedOut) {
        return new Party(busy, carried, carrying, inFlight, immune, optedOut);
    }

    private static void assertRefusal(Refusal expected, Optional<Refusal> actual) {
        Assertions.assertEquals(Optional.of(expected), actual, "the pickup must be refused with " + expected);
    }
}
