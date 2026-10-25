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
package net.onelitefeather.titan.feature.settings;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoreWrapTest {

    @DisplayName("A text that fits the width stays on one line")
    @Test
    void shortTextIsOneLine() {
        Assertions.assertEquals(List.of("Click to switch"), LoreWrap.wrap("Click to switch", 30), "a short text must not be wrapped");
    }

    @DisplayName("A long text is wrapped to lines of at most the width")
    @Test
    void longTextIsWrappedWithinWidth() {
        List<String> lines = LoreWrap.wrap("Tag other players with a left-click. Whoever is hit becomes the next tagger.", 30);
        Assertions.assertTrue(lines.size() > 1, "a text longer than the width must take several lines");
        for (String line : lines) {
            Assertions.assertTrue(line.length() <= 30, "line '" + line + "' must have at most 30 characters");
        }
    }

    @DisplayName("Words are never split across lines")
    @Test
    void wordsStayIntact() {
        List<String> lines = LoreWrap.wrap("alpha beta gamma delta epsilon zeta eta theta iota kappa", 12);
        Assertions.assertEquals(List.of("alpha beta", "gamma delta", "epsilon zeta", "eta theta", "iota kappa"), lines, "words must be packed greedily without being split");
    }

    @DisplayName("A word longer than the width gets a line of its own")
    @Test
    void overlongWordGetsItsOwnLine() {
        List<String> lines = LoreWrap.wrap("short supercalifragilisticexpialidocious end", 10);
        Assertions.assertEquals(List.of("short", "supercalifragilisticexpialidocious", "end"), lines, "an over-long word must stay whole on its own line");
    }

    @DisplayName("Runs of whitespace and line breaks are collapsed")
    @Test
    void whitespaceIsNormalized() {
        Assertions.assertEquals(List.of("one two three"), LoreWrap.wrap("  one\t two \n\n three  ", 30), "whitespace must collapse to single spaces at the ends and between words");
    }

    @DisplayName("A blank text gives no lines")
    @Test
    void blankTextHasNoLines() {
        Assertions.assertEquals(List.of(), LoreWrap.wrap("   ", 30), "a blank text must give no lore lines");
    }
}
