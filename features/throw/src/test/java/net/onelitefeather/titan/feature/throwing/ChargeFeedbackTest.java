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

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChargeFeedbackTest {

    @DisplayName("The charge tone rises in pitch with the strength")
    @Test
    void pitchRisesWithStrength() {
        Assertions.assertTrue(ChargeFeedback.pitch(0.0) < ChargeFeedback.pitch(0.5), "half a charge must sound higher than none");
        Assertions.assertTrue(ChargeFeedback.pitch(0.5) < ChargeFeedback.pitch(1.0), "a full charge must sound higher than half");
    }

    @DisplayName("The charge tone plays every five ticks and at no other tick")
    @Test
    void toneIsDueEveryFiveTicks() {
        Assertions.assertTrue(ChargeFeedback.soundDue(0), "the first tick of a charge plays the tone");
        Assertions.assertTrue(ChargeFeedback.soundDue(5), "tick 5 plays the tone");
        Assertions.assertFalse(ChargeFeedback.soundDue(3), "tick 3 must stay silent");
    }

    @DisplayName("The preview is refreshed every second tick")
    @Test
    void previewIsDueEverySecondTick() {
        Assertions.assertTrue(ChargeFeedback.previewDue(4), "an even tick refreshes the preview");
        Assertions.assertFalse(ChargeFeedback.previewDue(5), "an odd tick keeps the last preview");
    }

    @DisplayName("The actionbar bar grows with the strength and is full at strength one")
    @Test
    void barGrowsWithStrength() {
        Assertions.assertEquals(0, ChargeFeedback.filledCells(0.0), "no charge fills no cell");
        Assertions.assertEquals(5, ChargeFeedback.filledCells(0.5), "half a charge fills half of the cells");
        Assertions.assertEquals(ChargeFeedback.CELLS, ChargeFeedback.filledCells(1.0), "a full charge fills every cell");
        Assertions.assertTrue(ChargeFeedback.filledCells(0.3) <= ChargeFeedback.filledCells(0.7), "a stronger charge must never show fewer cells");
    }

    @DisplayName("The meter colour runs from green at no charge through yellow to red at a full charge")
    @Test
    void colourRunsFromGreenThroughYellowToRed() {
        Assertions.assertEquals(TextColor.color(0x55FF55), ChargeFeedback.colour(0.0), "no charge must be green");
        Assertions.assertEquals(TextColor.color(0xFFFF55), ChargeFeedback.colour(0.5), "half a charge must be yellow");
        Assertions.assertEquals(TextColor.color(0xFF5555), ChargeFeedback.colour(1.0), "a full charge must be red");
    }

    @DisplayName("A low charge is greener than red and a high charge is redder than green")
    @Test
    void lowIsGreenHighIsRed() {
        TextColor low = ChargeFeedback.colour(0.1);
        TextColor high = ChargeFeedback.colour(0.9);

        Assertions.assertTrue(low.green() > low.red(), "a low charge must look green, was " + low);
        Assertions.assertTrue(high.red() > high.green(), "a high charge must look red, was " + high);
    }

    @DisplayName("The meter shows its cells and the percentage of the charge")
    @Test
    void meterShowsCellsAndPercentage() {
        String text = PlainTextComponentSerializer.plainText().serialize(ChargeFeedback.meter(0.75));

        Assertions.assertEquals(ChargeFeedback.CELLS, text.replace(" 75%", "").length(), "the meter must span every cell, showed " + text);
        Assertions.assertTrue(text.endsWith(" 75%"), "the meter must end with the percentage, showed " + text);
    }

    @DisplayName("The meter's filled cells come first")
    @Test
    void meterFillsFromTheLeft() {
        String text = PlainTextComponentSerializer.plainText().serialize(ChargeFeedback.meter(0.5));

        Assertions.assertTrue(text.startsWith("█".repeat(5) + "░".repeat(5)), "half a charge must fill five cells from the left, showed " + text);
    }
}
