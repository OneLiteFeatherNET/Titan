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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class ModeTest {

    private static String plain(Mode mode) {
        return PlainTextComponentSerializer.plainText().serialize(mode.label());
    }

    @Test
    void theModesCycleFromEasyThroughMediumAndHardBackToEasy() {
        assertEquals(Mode.MEDIUM, Mode.EASY.next());
        assertEquals(Mode.HARD, Mode.MEDIUM.next());
        assertEquals(Mode.EASY, Mode.HARD.next());
    }

    @Test
    void easyUnlocksOnlyFullBlocksAndSlabs() {
        assertEquals(List.of(Surface.FULL), Mode.EASY.unlockedAt(9));
        assertEquals(List.of(Surface.FULL, Surface.SLAB), Mode.EASY.unlockedAt(10));
        assertEquals(List.of(Surface.FULL, Surface.SLAB), Mode.EASY.unlockedAt(10_000), "the narrow shapes never come");
    }

    @Test
    void mediumKeepsTheShapeThresholdsOfTheStandardRun() {
        List<Surface> shapes = List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.FENCE, Surface.PANE, Surface.POST);

        assertEquals(List.of(0, 10, 10, 25, 25, 40), shapes.stream().map(Mode.MEDIUM::minScore).toList());
    }

    @Test
    void hardUnlocksTheShapesAtFiveTenAndTwenty() {
        assertEquals(List.of(Surface.FULL), Mode.HARD.unlockedAt(4));
        assertEquals(List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB), Mode.HARD.unlockedAt(5));
        assertEquals(List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.FENCE, Surface.PANE), Mode.HARD.unlockedAt(10));
        assertEquals(List.of(Surface.values()), Mode.HARD.unlockedAt(20));
    }

    @Test
    void theClimbGetsSteeperFromEasyToHard() {
        assertEquals(List.of(160.0, 80.0, 40.0), List.of(Mode.EASY.scale(), Mode.MEDIUM.scale(), Mode.HARD.scale()));
    }

    @Test
    void easyAllowsGapsOfTwoWhileTheOthersAllowFourFlatAndThreeUp() {
        assertEquals(List.of(2, 2), List.of(Mode.EASY.maxGap(), Mode.EASY.maxGapAscent()));
        assertEquals(List.of(4, 3), List.of(Mode.MEDIUM.maxGap(), Mode.MEDIUM.maxGapAscent()));
        assertEquals(List.of(4, 3), List.of(Mode.HARD.maxGap(), Mode.HARD.maxGapAscent()));
    }

    @Test
    void anAscentCostsThreeTimesAsMuchOnlyInEasy() {
        assertEquals(List.of(3.0, 1.0, 1.0), List.of(Mode.EASY.ascentWeight(), Mode.MEDIUM.ascentWeight(), Mode.HARD.ascentWeight()));
    }

    @Test
    void theNamesAreLanguageNeutralAndColouredGreenYellowRed() {
        assertEquals(List.of("Easy", "Medium", "Hard"), List.of(plain(Mode.EASY), plain(Mode.MEDIUM), plain(Mode.HARD)));
        assertEquals(NamedTextColor.GREEN, Mode.EASY.label().color());
        assertEquals(NamedTextColor.YELLOW, Mode.MEDIUM.label().color());
        assertEquals(NamedTextColor.RED, Mode.HARD.label().color());
    }

    @Test
    void theHardestJumpOfEasyIsASlabOverAGapOfTwo() {
        double hardest = Jump.maxCost(Mode.EASY, Mode.EASY.unlockedAt(10_000));

        assertEquals(2.0 * Surface.SLAB.typeCost() + 1.5, hardest);
        assertNotEquals(Jump.MAX_COST, hardest, "easy has its own ceiling");
    }
}
