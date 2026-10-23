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
package net.onelitefeather.titan.feature.dance;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The pure model of the dances: sequences, the catalogue, the tick counter, the movement rule. */
class DanceModelTest {

    @DisplayName("Every dance does something before its length and nothing at its end tick")
    @Test
    void everyDanceActsInsideItsLengthOnly() {
        for (Dance dance : Dance.values()) {
            DanceSequence sequence = dance.sequence();
            Assertions.assertTrue(IntStream.range(0, sequence.lengthTicks()).anyMatch(tick -> !sequence.actionsAt(tick).isEmpty()), dance.id() + " must do something while it lasts");
            Assertions.assertEquals(List.of(), sequence.actionsAt(sequence.lengthTicks()), dance.id() + " must not act at its end tick");
        }
    }

    @DisplayName("every repeats an action at each period from its start, up to but excluding the end")
    @Test
    void everyRepeatsUpToTheEndExclusive() {
        DanceSequence sequence = DanceSequence.lasting(30).every(10, 0, 30, DanceAction.NOTE).build();

        Assertions.assertEquals(List.of(DanceAction.NOTE), sequence.actionsAt(0), "a note at tick 0");
        Assertions.assertEquals(List.of(DanceAction.NOTE), sequence.actionsAt(10), "a note at tick 10");
        Assertions.assertEquals(List.of(DanceAction.NOTE), sequence.actionsAt(20), "a note at tick 20");
        Assertions.assertEquals(List.of(), sequence.actionsAt(30), "no note at the end tick, which is excluded");
    }

    @DisplayName("alternating starts with the first action and switches on every period")
    @Test
    void alternatingSwitchesEveryPeriod() {
        DanceSequence sequence = DanceSequence.lasting(20).alternating(5, 0, 20, DanceAction.SNEAK, DanceAction.STAND).build();

        Assertions.assertEquals(List.of(DanceAction.SNEAK), sequence.actionsAt(0), "the first action comes first");
        Assertions.assertEquals(List.of(DanceAction.STAND), sequence.actionsAt(5), "the second action follows");
        Assertions.assertEquals(List.of(DanceAction.SNEAK), sequence.actionsAt(10), "and the first again");
    }

    @DisplayName("A step outside the length of its dance is rejected when the sequence is built")
    @Test
    void stepOutsideTheLengthIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DanceSequence.lasting(10).at(10, DanceAction.HEART), "a step at the end tick is outside the dance");
    }

    @DisplayName("A period of zero ticks is rejected, since it would never end")
    @Test
    void zeroPeriodIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DanceSequence.lasting(10).every(0, 0, 10, DanceAction.NOTE), "a zero period must not build");
    }

    @DisplayName("The catalogue has four dances with unique ids")
    @Test
    void catalogueHasFourDancesWithUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (Dance dance : Dance.values()) {
            ids.add(dance.id());
        }
        Assertions.assertEquals(Set.of("wave", "groove", "party", "spin"), ids, "the four dances and their ids");
        Assertions.assertEquals(4, ids.size(), "ids must be unique");
    }

    @DisplayName("Only spin needs a permission; the other dances are open to everybody")
    @Test
    void onlySpinNeedsAPermission() {
        Assertions.assertEquals(Optional.of("titan.dance.spin"), Dance.SPIN.permission(), "spin needs titan.dance.spin");
        Assertions.assertEquals(Optional.empty(), Dance.WAVE.permission(), "wave is free");
        Assertions.assertEquals(Optional.empty(), Dance.GROOVE.permission(), "groove is free");
        Assertions.assertEquals(Optional.empty(), Dance.PARTY.permission(), "party is free");
    }

    @DisplayName("A dance is found by its id, and an unknown id finds nothing")
    @Test
    void danceIsFoundById() {
        Assertions.assertEquals(Optional.of(Dance.PARTY), Dance.byId("party"), "party by its id");
        Assertions.assertEquals(Optional.empty(), Dance.byId("moonwalk"), "an unknown id must find nothing");
    }

    @DisplayName("A session reports the actions of each tick and finishes exactly when the length is reached")
    @Test
    void sessionFinishesAtTheLength() {
        DanceSession session = new DanceSession(Dance.SPIN);

        DanceSession.Frame first = session.advance();
        Assertions.assertFalse(first.finished(), "the first tick is still part of the dance");
        Assertions.assertTrue(first.actions().contains(DanceAction.SPIN), "spin starts on the first tick");

        for (int tick = 1; tick < Dance.SPIN.sequence().lengthTicks(); tick++) {
            Assertions.assertFalse(session.advance().finished(), "tick " + tick + " is still part of the dance");
        }
        DanceSession.Frame end = session.advance();
        Assertions.assertTrue(end.finished(), "the dance is over once its length is reached");
        Assertions.assertEquals(List.of(), end.actions(), "a finished dance performs nothing");
    }

    @DisplayName("Movement within the threshold is not movement, however the dancer turns")
    @Test
    void smallMovementIsNotMovement() {
        Pos anchor = new Pos(10, 64, 10);
        Assertions.assertFalse(DanceSession.movedBeyond(anchor, new Pos(10.1, 64, 10.1)), "a tenth of a block is within the threshold");
        Assertions.assertFalse(DanceSession.movedBeyond(anchor, new Pos(10, 64, 10, 90, 0)), "a turn without a step is not movement");
    }

    @DisplayName("A horizontal step beyond the threshold counts as movement")
    @Test
    void horizontalStepBeyondTheThresholdIsMovement() {
        Pos anchor = new Pos(10, 64, 10);
        Assertions.assertTrue(DanceSession.movedBeyond(anchor, new Pos(10.3, 64, 10)), "more than a quarter of a block sideways is movement");
    }

    @DisplayName("A vertical move, such as a hop, does not count as movement")
    @Test
    void verticalMoveIsNotMovement() {
        Pos anchor = new Pos(10, 64, 10);
        Assertions.assertFalse(DanceSession.movedBeyond(anchor, new Pos(10, 66, 10)), "a jump must not stop the dance");
    }
}
