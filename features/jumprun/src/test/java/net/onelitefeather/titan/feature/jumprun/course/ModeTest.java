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
package net.onelitefeather.titan.feature.jumprun.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class ModeTest {

    private static String plain(Mode mode) {
        return PlainTextComponentSerializer.plainText().serialize(mode.label());
    }

    @Test
    void theModesCycleThroughAllFiveInTheOrderOfTheEnumAndBackToEasy() {
        assertEquals(List.of(Mode.EASY, Mode.MEDIUM, Mode.HARD, Mode.RAINBOW, Mode.ULTRA), List.of(Mode.values()));
        assertEquals(Mode.MEDIUM, Mode.EASY.next());
        assertEquals(Mode.HARD, Mode.MEDIUM.next());
        assertEquals(Mode.RAINBOW, Mode.HARD.next());
        assertEquals(Mode.ULTRA, Mode.RAINBOW.next());
        assertEquals(Mode.EASY, Mode.ULTRA.next());
    }

    private static Set<TextColor> coloursOf(Component component) {
        Set<TextColor> colours = new HashSet<>();
        if (component.color() != null) {
            colours.add(component.color());
        }
        component.children().forEach(child -> colours.addAll(coloursOf(child)));
        return colours;
    }

    @Test
    void rainbowGeneratesLikeMedium() {
        assertSameGeneration(Mode.MEDIUM, Mode.RAINBOW);
    }

    @Test
    void ultraGeneratesLikeHard() {
        assertSameGeneration(Mode.HARD, Mode.ULTRA);
    }

    private static void assertSameGeneration(Mode expected, Mode actual) {
        assertEquals(List.of(expected.scale(), expected.maxGap(), expected.maxGapAscent(), expected.ascentWeight()), List.of(actual.scale(), actual.maxGap(), actual.maxGapAscent(), actual.ascentWeight()), "difficulty parameters");
        for (Surface surface : Surface.values()) {
            assertEquals(expected.minScore(surface), actual.minScore(surface), "unlock score of " + surface);
        }
    }

    @Test
    void onlyRainbowRerollsTheMaterialAndOnlyUltraTheCourse() {
        assertEquals(List.of(Mode.Reroll.NONE, Mode.Reroll.NONE, Mode.Reroll.NONE, Mode.Reroll.MATERIAL, Mode.Reroll.COURSE), Arrays.stream(Mode.values()).map(Mode::reroll).toList());
    }

    @Test
    void onlyUltraShowsNoOutline() {
        assertEquals(List.of(true, true, true, true, false), Arrays.stream(Mode.values()).map(Mode::outlined).toList());
    }

    @Test
    void rainbowIsNamedInRainbowColoursAndUltraInBoldDarkRed() {
        assertEquals("Rainbow", plain(Mode.RAINBOW));
        assertTrue(coloursOf(Mode.RAINBOW.label()).size() > 1, "more than one colour over the letters");
        assertEquals(NamedTextColor.DARK_RED, Mode.ULTRA.label().color());
        assertEquals(TextDecoration.State.TRUE, Mode.ULTRA.label().decoration(TextDecoration.BOLD));
        assertEquals("Ultra", plain(Mode.ULTRA));
    }

    @Test
    void easyUnlocksOnlyFullBlocksAndSlabs() {
        assertEquals(List.of(Surface.FULL), Mode.EASY.unlockedAt(9));
        assertEquals(List.of(Surface.FULL, Surface.SLAB), Mode.EASY.unlockedAt(10));
        assertEquals(List.of(Surface.FULL, Surface.SLAB), Mode.EASY.unlockedAt(10_000), "the narrow shapes never come");
    }

    @Test
    void mediumUnlocksTheWideShapesAtTen() {
        assertEquals(List.of(0, 10, 10, 10, 10, 10), List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.STAIRS, Surface.CARPET, Surface.SNOW).stream().map(Mode.MEDIUM::minScore).toList());
    }

    @Test
    void mediumNeverUnlocksANarrowShape() {
        for (Surface surface : List.of(Surface.FENCE, Surface.PANE, Surface.POST, Surface.HEAD, Surface.FLOWER_POT, Surface.CANDLE)) {
            assertEquals(Integer.MAX_VALUE, Mode.MEDIUM.minScore(surface), surface + " never comes in medium");
            assertFalse(Mode.MEDIUM.unlockedAt(10_000).contains(surface), surface + " stays out of medium at any score");
        }
    }

    @Test
    void theHardestMediumJumpScalesDownWithoutTheNarrowShapes() {
        assertEquals(2.0 * Surface.STAIRS.typeCost() + 1.5 * (Mode.MEDIUM.maxGap() - 1), Jump.maxCost(Mode.MEDIUM, Mode.MEDIUM.unlockedAt(10_000)));
    }

    @Test
    void hardAndUltraStillUnlockTheNarrowShapes() {
        assertEquals(List.of(10, 10, 20, 10, 10, 20), List.of(Surface.FENCE, Surface.PANE, Surface.POST, Surface.HEAD, Surface.FLOWER_POT, Surface.CANDLE).stream().map(Mode.HARD::minScore).toList());
        assertEquals(Mode.HARD.unlockedAt(10_000), Mode.ULTRA.unlockedAt(10_000));
    }

    @Test
    void hardUnlocksTheNewShapesAtFiveTenAndTwenty() {
        assertEquals(List.of(5, 5, 5, 10, 10, 20), List.of(Surface.STAIRS, Surface.CARPET, Surface.SNOW, Surface.HEAD, Surface.FLOWER_POT, Surface.CANDLE).stream().map(Mode.HARD::minScore).toList());
    }

    @Test
    void easyUnlocksNoNewShape() {
        for (Surface surface : List.of(Surface.STAIRS, Surface.CARPET, Surface.SNOW, Surface.HEAD, Surface.FLOWER_POT, Surface.CANDLE)) {
            assertEquals(Integer.MAX_VALUE, Mode.EASY.minScore(surface), surface + " never comes in easy");
        }
    }

    @Test
    void noNewShapeComesBelowTheFirstThresholdOfItsMode() {
        List<Surface> wide = List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.STAIRS, Surface.CARPET, Surface.SNOW);

        assertEquals(List.of(Surface.FULL), Mode.MEDIUM.unlockedAt(9), "medium below 10");
        assertEquals(List.of(Surface.FULL), Mode.HARD.unlockedAt(4), "hard below 5");
        assertEquals(wide, Mode.MEDIUM.unlockedAt(10_000), "medium keeps only the wide shapes");
    }

    @Test
    void hardUnlocksTheShapesAtFiveTenAndTwenty() {
        assertEquals(List.of(Surface.FULL), Mode.HARD.unlockedAt(4));
        assertEquals(List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.STAIRS, Surface.CARPET, Surface.SNOW), Mode.HARD.unlockedAt(5));
        assertEquals(List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.STAIRS, Surface.CARPET, Surface.SNOW, Surface.FENCE, Surface.PANE, Surface.HEAD, Surface.FLOWER_POT), Mode.HARD.unlockedAt(10));
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

    @Test
    void aLadderUnlocksInMediumAtScore30AndInHardAtScore15() {
        assertFalse(Mode.MEDIUM.climbsUnlockedAt(29).contains(Climb.Kind.LADDER), "medium at 29");
        assertTrue(Mode.MEDIUM.climbsUnlockedAt(30).contains(Climb.Kind.LADDER), "medium at 30");
        assertFalse(Mode.HARD.climbsUnlockedAt(14).contains(Climb.Kind.LADDER), "hard at 14");
        assertTrue(Mode.HARD.climbsUnlockedAt(15).contains(Climb.Kind.LADDER), "hard at 15");
    }

    @Test
    void aVineUnlocksInMediumAtScore40AndInHardAtScore25() {
        assertFalse(Mode.MEDIUM.climbsUnlockedAt(39).contains(Climb.Kind.VINE), "medium at 39");
        assertTrue(Mode.MEDIUM.climbsUnlockedAt(40).contains(Climb.Kind.VINE), "medium at 40");
        assertFalse(Mode.HARD.climbsUnlockedAt(24).contains(Climb.Kind.VINE), "hard at 24");
        assertTrue(Mode.HARD.climbsUnlockedAt(25).contains(Climb.Kind.VINE), "hard at 25");
    }

    @Test
    void easyNeverUnlocksAClimb() {
        for (int score = 0; score <= 200; score++) {
            assertTrue(Mode.EASY.climbsUnlockedAt(score).isEmpty(), "easy at score " + score);
        }
    }
}
