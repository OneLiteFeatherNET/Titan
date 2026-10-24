/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.tag;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Rules of one lobby's tag game. Time is passed in as millis and the random source is seeded, so
 * every test is repeatable and never waits.
 */
class TagGameTest {

    private static final long START = 1_000_000L;
    private static final UUID ANNA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID CARL = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID DORA = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    private static TagGame game() {
        return new TagGame(new Random(42L));
    }

    /**
     * Always picks the first candidate. The game draws from the players in join order, so the
     * player who joins first is the one who starts as tagger.
     */
    private static final class FirstCandidate extends Random {

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    /** A game in which {@code tagger} holds the role; {@code others} join after it. */
    private static TagGame gameWithTagger(UUID tagger, UUID... others) {
        TagGame game = new TagGame(new FirstCandidate());
        game.join(tagger, START);
        for (UUID other : others) {
            game.join(other, START);
        }
        Assertions.assertEquals(Optional.of(tagger), game.tagger(), "test setup must start with the given tagger");
        return game;
    }

    @DisplayName("A second active player makes the game start with exactly one tagger")
    @Test
    void secondActivePlayerStartsTheGame() {
        TagGame game = game();

        game.join(ANNA, START);
        Assertions.assertTrue(game.tagger().isEmpty(), "one player alone must not have a tagger");
        game.join(BEN, START);

        Assertions.assertTrue(game.tagger().isPresent(), "the second player must start the game");
        Assertions.assertTrue(game.tagger().get().equals(ANNA) || game.tagger().get().equals(BEN), "the tagger must be one of the two players");
    }

    @DisplayName("When the tagger leaves, another active player takes the role")
    @Test
    void taggerLeavingHandsOverTheRole() {
        TagGame game = gameWithTagger(ANNA, BEN, CARL);

        game.leave(ANNA, START);

        Assertions.assertTrue(game.tagger().isPresent() && !game.tagger().get().equals(ANNA), "the role must move to a remaining player");
        Assertions.assertTrue(game.isActive(game.tagger().get()), "the new tagger must be active");
    }

    @DisplayName("A player leaving who is not the tagger changes nothing")
    @Test
    void leavingNonTaggerChangesNothing() {
        TagGame game = gameWithTagger(ANNA, BEN, CARL);

        game.leave(BEN, START);

        Assertions.assertEquals(Optional.of(ANNA), game.tagger(), "the tagger must keep the role when someone else leaves");
    }

    @DisplayName("With one player left after a leave there is no tagger")
    @Test
    void oneLeftPlayerMeansNoTagger() {
        TagGame game = gameWithTagger(ANNA, BEN);

        game.leave(BEN, START);

        Assertions.assertTrue(game.tagger().isEmpty(), "a single active player cannot be chased");
    }

    @DisplayName("The tagger hitting an active player passes the role to that player")
    @Test
    void taggerHitPassesTheRole() {
        TagGame game = gameWithTagger(ANNA, BEN);

        TagOutcome outcome = game.hit(ANNA, BEN, START);

        Assertions.assertEquals(TagOutcome.PASSED, outcome, "a tagger's hit on another active player must pass the role");
        Assertions.assertEquals(Optional.of(BEN), game.tagger(), "the target must hold the role after the hit");
    }

    @DisplayName("Hitting back the player who just tagged you is refused within five seconds")
    @Test
    void hittingBackWithinCooldownIsRefused() {
        TagGame game = gameWithTagger(ANNA, BEN);
        game.hit(ANNA, BEN, START);

        TagOutcome outcome = game.hit(BEN, ANNA, START + TagGame.HIT_COOLDOWN_MILLIS - 1);

        Assertions.assertEquals(TagOutcome.COOLDOWN, outcome, "the reverse hit within the cooldown must be refused");
        Assertions.assertEquals(Optional.of(BEN), game.tagger(), "the role must stay with the new tagger");
    }

    @DisplayName("Hitting back is allowed once five seconds have passed")
    @Test
    void hittingBackAfterCooldownPassesTheRole() {
        TagGame game = gameWithTagger(ANNA, BEN);
        game.hit(ANNA, BEN, START);

        TagOutcome outcome = game.hit(BEN, ANNA, START + TagGame.HIT_COOLDOWN_MILLIS);

        Assertions.assertEquals(TagOutcome.PASSED, outcome, "the reverse hit must be allowed exactly when the cooldown ends");
        Assertions.assertEquals(Optional.of(ANNA), game.tagger(), "the role must move back after the cooldown");
    }

    @DisplayName("The cooldown only blocks the pair that just swapped the role")
    @Test
    void cooldownOnlyBlocksThatPair() {
        TagGame game = gameWithTagger(ANNA, BEN, CARL);
        game.hit(ANNA, BEN, START);
        game.hit(BEN, CARL, START + 1_000);

        TagOutcome outcome = game.hit(CARL, ANNA, START + 2_000);

        Assertions.assertEquals(TagOutcome.PASSED, outcome, "a different pair must not be affected by the cooldown");
    }

    @DisplayName("A hit by a player who is not the tagger changes nothing")
    @Test
    void hitWithoutTheRoleChangesNothing() {
        TagGame game = gameWithTagger(CARL, ANNA, BEN);

        TagOutcome outcome = game.hit(ANNA, BEN, START);

        Assertions.assertEquals(TagOutcome.IGNORED, outcome, "a hit between two non-taggers must not count");
        Assertions.assertEquals(Optional.of(CARL), game.tagger(), "the role must stay where it was");
    }

    @DisplayName("A hit on a player who is not active is ignored")
    @Test
    void hitOnInactivePlayerIsIgnored() {
        TagGame game = gameWithTagger(ANNA, BEN);

        TagOutcome outcome = game.hit(ANNA, DORA, START);

        Assertions.assertEquals(TagOutcome.IGNORED, outcome, "a player outside the game must not be tagged");
        Assertions.assertEquals(Optional.of(ANNA), game.tagger(), "the role must stay with the tagger");
    }

    @DisplayName("A tagger who stays still for two minutes loses the role to another player")
    @Test
    void idleTaggerLosesTheRole() {
        TagGame game = gameWithTagger(ANNA, BEN, CARL);

        game.tick(START + TagGame.INACTIVITY_MILLIS);

        Assertions.assertTrue(game.tagger().isPresent() && !game.tagger().get().equals(ANNA), "an idle tagger must hand the role to someone else");
    }

    @DisplayName("Movement before the limit keeps the role with the tagger")
    @Test
    void movementResetsTheIdleTimer() {
        TagGame game = gameWithTagger(ANNA, BEN, CARL);

        game.touch(ANNA, START + 60_000);
        game.tick(START + 60_000 + TagGame.INACTIVITY_MILLIS - 1);

        Assertions.assertEquals(Optional.of(ANNA), game.tagger(), "the tagger must keep the role while moving within the limit");
    }

    @DisplayName("A hit by the tagger counts as activity")
    @Test
    void hitResetsTheIdleTimer() {
        TagGame game = gameWithTagger(ANNA, BEN, CARL);

        game.hit(ANNA, CARL, START + 60_000);
        game.tick(START + 60_000 + TagGame.INACTIVITY_MILLIS - 1);

        Assertions.assertEquals(Optional.of(CARL), game.tagger(), "the tagger's hit at 60 s must have passed the role to carl, with a fresh timer");
    }

    @DisplayName("An idle player who is not the tagger keeps the game and the role")
    @Test
    void idleNonTaggerStaysInTheGame() {
        TagGame game = gameWithTagger(ANNA, BEN, CARL);
        long later = START + 10 * TagGame.INACTIVITY_MILLIS;
        game.touch(ANNA, later - 1_000);

        game.tick(later);

        Assertions.assertTrue(game.isActive(BEN), "an idle player who is not tagged must stay active");
        Assertions.assertEquals(Optional.of(ANNA), game.tagger(), "an idle player who is not tagged must not change the tagger");
    }

    @DisplayName("Joining while a tagger exists leaves the role where it is")
    @Test
    void joiningDoesNotMoveTheRole() {
        TagGame game = gameWithTagger(ANNA, BEN);

        game.join(CARL, START);

        Assertions.assertEquals(Optional.of(ANNA), game.tagger(), "a late joiner must not take the role from the tagger");
    }

    @DisplayName("Joining twice does not add the player twice")
    @Test
    void joiningTwiceIsIdempotent() {
        TagGame game = game();
        game.join(ANNA, START);

        boolean again = game.join(ANNA, START);

        Assertions.assertFalse(again, "joining an active player again must report no change");
        Assertions.assertTrue(game.tagger().isEmpty(), "one player joined twice is still one player");
    }
}
