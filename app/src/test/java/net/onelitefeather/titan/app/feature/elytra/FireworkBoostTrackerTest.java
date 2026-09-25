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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ported from Voyager's {@code FireworkBoostTrackerTest}
 * ({@code net.elytrarace.voyager.platform.flight.FireworkBoostTrackerTest}): the burn is pure
 * arithmetic and is tested as such - no {@code Env}, no {@code Player}, no rocket. F.I.R.S.T., test
 * pyramid: this is the pure unit layer for {@link FireworkBoostTracker}; {@link ElytraModuleTest}
 * covers the entity/event half through a real {@code ModuleHarness}.
 *
 * <h2>What the fixtures are built to tell apart</h2>
 *
 * <p>{@link #SHORT} burns 4 and cools down 9; {@link #LONG} burns 7 and cools down 11. Neither
 * burn divides the other, neither cooldown is twice its burn, and no number appears in both - so a
 * tracker that read the burn where it meant the cooldown, or that kept the first configuration it
 * ever saw, produces a count that is in neither column.
 *
 * <p>Two players, never one. Every per-player assertion is made against a second player whose
 * state is deliberately different at that moment, because a tracker keyed on nothing at all would
 * pass every single-player test in this file.
 */
class FireworkBoostTrackerTest {

    private static final ElytraConfig SHORT = new ElytraConfig(4, 9);
    private static final ElytraConfig LONG = new ElytraConfig(7, 11);

    private static final UUID ADA = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    private static final UUID BEN = UUID.fromString("00000000-0000-4000-8000-0000000000b2");

    private static final boolean GLIDING = true;
    private static final boolean ON_FOOT = false;

    private final FireworkBoostTracker tracker = new FireworkBoostTracker();

    // ------------------------------------------------------------------------------------------
    // The burn
    // ------------------------------------------------------------------------------------------

    @DisplayName("A burn starts on the tick it is asked for and is reported at its full length")
    @Test
    void aBurnStartsOnTheTickItIsAskedForAndIsReportedAtItsFullLength() {
        Assertions.assertFalse(this.tracker.burning(ADA));

        Assertions.assertTrue(this.tracker.requestBoost(ADA, SHORT, GLIDING));

        Assertions.assertTrue(this.tracker.burning(ADA));
        Assertions.assertEquals(4, this.tracker.ticksRemaining(ADA), "the tick it starts on counts, so the first reading is the whole burn");
    }

    @DisplayName("A burn is reported on exactly as many ticks as it was configured for")
    @Test
    void aBurnIsReportedOnExactlyAsManyTicksAsItWasConfiguredFor() {
        this.tracker.requestBoost(ADA, SHORT, GLIDING);

        int boostedTicks = 0;
        for (int tick = 0; tick < 20; tick++) {
            if (this.tracker.burning(ADA)) {
                boostedTicks++;
            }
            this.tracker.advance();
        }

        Assertions.assertEquals(SHORT.burnDurationTicks(), boostedTicks);
    }

    @DisplayName("A burn counts down one tick at a time and then stops")
    @Test
    void aBurnCountsDownOneTickAtATimeAndThenStops() {
        this.tracker.requestBoost(ADA, SHORT, GLIDING);

        Assertions.assertEquals(4, this.tracker.ticksRemaining(ADA));
        this.tracker.advance();
        Assertions.assertEquals(3, this.tracker.ticksRemaining(ADA));
        this.tracker.advance();
        this.tracker.advance();
        Assertions.assertEquals(1, this.tracker.ticksRemaining(ADA));
        this.tracker.advance();
        Assertions.assertEquals(0, this.tracker.ticksRemaining(ADA));
        Assertions.assertFalse(this.tracker.burning(ADA));

        this.tracker.advance();
        Assertions.assertEquals(0, this.tracker.ticksRemaining(ADA), "an ended burn stays ended rather than counting past zero");
    }

    @DisplayName("A burn runs the length of the configuration it was started with")
    @Test
    void aBurnRunsTheLengthOfTheConfigurationItWasStartedWith() {
        this.tracker.requestBoost(ADA, LONG, GLIDING);
        this.tracker.requestBoost(BEN, SHORT, GLIDING);

        for (int tick = 0; tick < 4; tick++) {
            this.tracker.advance();
        }

        Assertions.assertEquals(0, this.tracker.ticksRemaining(BEN), "the 4-tick burn is over");
        Assertions.assertEquals(3, this.tracker.ticksRemaining(ADA), "the 7-tick burn has 3 left, on its own tuning");
    }

    // ------------------------------------------------------------------------------------------
    // The cooldown
    // ------------------------------------------------------------------------------------------

    @DisplayName("A second boost during the cooldown is refused and changes nothing")
    @Test
    void aSecondBoostDuringTheCooldownIsRefusedAndChangesNothing() {
        this.tracker.requestBoost(ADA, SHORT, GLIDING);
        this.tracker.advance();

        Assertions.assertFalse(this.tracker.requestBoost(ADA, LONG, GLIDING));

        Assertions.assertEquals(3, this.tracker.ticksRemaining(ADA), "the refused request did not restart the burn, nor lengthen it to LONG's 7");
    }

    @DisplayName("The cooldown is measured from the burn's start, so it outlasts the burn")
    @Test
    void theCooldownIsMeasuredFromTheBurnsStartSoItOutlastsTheBurn() {
        this.tracker.requestBoost(ADA, SHORT, GLIDING);
        Assertions.assertEquals(9, this.tracker.cooldownTicksRemaining(ADA));

        for (int tick = 0; tick < 4; tick++) {
            this.tracker.advance();
        }

        Assertions.assertFalse(this.tracker.burning(ADA), "the 4-tick burn is over");
        Assertions.assertEquals(5, this.tracker.cooldownTicksRemaining(ADA), "9 measured from the start leaves 5 after a burn of 4");
        Assertions.assertFalse(this.tracker.requestBoost(ADA, SHORT, GLIDING));
    }

    @DisplayName("A boost is allowed again on the tick the cooldown runs out, and not before")
    @Test
    void aBoostIsAllowedAgainOnTheTickTheCooldownRunsOutAndNotBefore() {
        this.tracker.requestBoost(ADA, SHORT, GLIDING);
        for (int tick = 0; tick < 8; tick++) {
            this.tracker.advance();
        }

        Assertions.assertEquals(1, this.tracker.cooldownTicksRemaining(ADA));
        Assertions.assertFalse(this.tracker.requestBoost(ADA, SHORT, GLIDING), "one tick of cooldown left is still a refusal");

        this.tracker.advance();

        Assertions.assertEquals(0, this.tracker.cooldownTicksRemaining(ADA));
        Assertions.assertTrue(this.tracker.requestBoost(ADA, SHORT, GLIDING));
    }

    // ------------------------------------------------------------------------------------------
    // Who may boost
    // ------------------------------------------------------------------------------------------

    @DisplayName("A boost asked for while not gliding is refused and costs no cooldown")
    @Test
    void aBoostAskedForWhileNotGlidingIsRefusedAndCostsNoCooldown() {
        Assertions.assertFalse(this.tracker.requestBoost(ADA, SHORT, ON_FOOT));

        Assertions.assertFalse(this.tracker.burning(ADA));
        Assertions.assertEquals(0, this.tracker.cooldownTicksRemaining(ADA), "a refused boost must not spend the cooldown of one that happened");
        Assertions.assertTrue(this.tracker.requestBoost(ADA, SHORT, GLIDING));
    }

    @DisplayName("One player's burn is not another's")
    @Test
    void onePlayersBurnIsNotAnothers() {
        this.tracker.requestBoost(ADA, LONG, GLIDING);

        Assertions.assertTrue(this.tracker.burning(ADA));
        Assertions.assertFalse(this.tracker.burning(BEN));
        Assertions.assertEquals(0, this.tracker.ticksRemaining(BEN));
        Assertions.assertTrue(this.tracker.requestBoost(BEN, SHORT, GLIDING), "Ada's cooldown is not Ben's");
        Assertions.assertEquals(7, this.tracker.ticksRemaining(ADA));
        Assertions.assertEquals(4, this.tracker.ticksRemaining(BEN));
    }

    // ------------------------------------------------------------------------------------------
    // Forgetting
    // ------------------------------------------------------------------------------------------

    @DisplayName("A player who disconnects (or lands) mid-burn is forgotten and takes nobody else with them")
    @Test
    void aPlayerWhoIsForgottenMidBurnTakesNobodyElseWithThem() {
        this.tracker.requestBoost(ADA, SHORT, GLIDING);
        this.tracker.requestBoost(BEN, LONG, GLIDING);
        this.tracker.advance();

        this.tracker.forget(ADA);

        Assertions.assertFalse(this.tracker.burning(ADA));
        Assertions.assertEquals(0, this.tracker.ticksRemaining(ADA));
        Assertions.assertEquals(0, this.tracker.cooldownTicksRemaining(ADA), "the cooldown goes with the burn, so landing and flying again is not held by one");
        Assertions.assertEquals(6, this.tracker.ticksRemaining(BEN), "the other player's burn is untouched");
    }

    @DisplayName("clear() for a player with no active boost is a safe no-op")
    @Test
    void forgetForAnUntrackedPlayerIsANoOp() {
        Assertions.assertDoesNotThrow(() -> this.tracker.forget(ADA));
    }

    @DisplayName("A player who never boosted is ready and counts nothing")
    @Test
    void aPlayerWhoNeverBoostedIsReadyAndCountsNothing() {
        this.tracker.advance();
        this.tracker.advance();

        Assertions.assertFalse(this.tracker.burning(ADA));
        Assertions.assertEquals(0, this.tracker.ticksRemaining(ADA));
        Assertions.assertEquals(0, this.tracker.cooldownTicksRemaining(ADA));
        Assertions.assertTrue(this.tracker.requestBoost(ADA, SHORT, GLIDING));
    }
}
