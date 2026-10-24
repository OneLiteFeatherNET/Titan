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
package net.onelitefeather.titan.buildsrc.quality;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit coverage for {@link PmdRatchet} on XML fixtures in a {@code @TempDir} (F.I.R.S.T.). */
class PmdRatchetTest {

    private static final String FILE = "features/sit/src/main/java/net/onelitefeather/titan/feature/sit/Sit.java";
    private static final String KEY = FILE + "#SystemPrintln";

    @TempDir
    Path root;

    @DisplayName("Two violations of one rule in one file are counted as two")
    @Test
    void countsViolationsPerFileAndRule() throws IOException {
        Path report = writeReport(
                "<file name=\"" + root.resolve(FILE) + "\">",
                "<violation rule=\"SystemPrintln\" beginline=\"10\">a</violation>",
                "<violation rule=\"SystemPrintln\" beginline=\"12\">b</violation>",
                "</file>");

        Map<String, Integer> counts = PmdRatchet.countViolations(report, root);

        Assertions.assertEquals(Map.of(KEY, 2), counts, "two violations of the rule in the file");
    }

    @DisplayName("Each violation keeps the line it was reported on, so a failure can point at it")
    @Test
    void violationLinesNameTheLineOfEachViolation() throws IOException {
        Path report = writeReport(
                "<file name=\"" + root.resolve(FILE) + "\">",
                "<violation rule=\"SystemPrintln\" beginline=\"10\">a</violation>",
                "<violation rule=\"SystemPrintln\" beginline=\"12\">b</violation>",
                "</file>");

        Map<String, List<Integer>> lines = PmdRatchet.violationLines(report, root);

        Assertions.assertEquals(Map.of(KEY, List.of(10, 12)), lines, "the lines of both violations");
    }

    @DisplayName("Two violations against a baseline of one fail the build")
    @Test
    void moreViolationsThanTheBaselineFail() {
        PmdRatchet.Outcome outcome = PmdRatchet.compare(Map.of(KEY, 2), Map.of(KEY, 1));

        Assertions.assertFalse(outcome.passed(), "two violations exceed a baseline of one");
        Assertions.assertTrue(outcome.failures().get(0).contains(KEY), "the failure names the file and the rule");
    }

    @DisplayName("Two violations against a baseline of two pass")
    @Test
    void violationsWithinTheBaselinePass() {
        PmdRatchet.Outcome outcome = PmdRatchet.compare(Map.of(KEY, 2), Map.of(KEY, 2));

        Assertions.assertTrue(outcome.passed(), "the violations are the accepted baseline");
        Assertions.assertTrue(outcome.shrinkable().isEmpty(), "nothing can shrink");
    }

    @DisplayName("Fewer violations than the baseline pass and report that the baseline can shrink")
    @Test
    void fewerViolationsThanTheBaselinePassAndSuggestShrinking() {
        PmdRatchet.Outcome outcome = PmdRatchet.compare(Map.of(KEY, 2), Map.of(KEY, 3));

        Assertions.assertTrue(outcome.passed(), "fewer violations are not a failure");
        Assertions.assertEquals(1, outcome.shrinkable().size(), "the baseline can shrink from 3 to 2");
    }

    @DisplayName("A violation in a file without a baseline entry fails the build")
    @Test
    void aViolationInANewFileFails() {
        PmdRatchet.Outcome outcome = PmdRatchet.compare(Map.of(KEY, 1), Map.of());

        Assertions.assertFalse(outcome.passed(), "a file without a baseline entry must not violate the rule");
    }

    @DisplayName("No violations and no baseline pass without any note")
    @Test
    void noViolationsPass() {
        PmdRatchet.Outcome outcome = PmdRatchet.compare(Map.of(), Map.of());

        Assertions.assertTrue(outcome.passed(), "a clean module passes");
        Assertions.assertTrue(outcome.shrinkable().isEmpty(), "nothing to report");
    }

    @DisplayName("A baseline written by the ratchet reads back with the same counts")
    @Test
    void baselineRoundTripsThroughItsFile() throws IOException {
        Path baseline = root.resolve("pmd-baseline.properties");
        Map<String, Integer> counts = Map.of(KEY, 2, "core/src/main/java/net/onelitefeather/titan/core/X.java#UnusedPrivateField", 1);

        PmdRatchet.writeBaseline(baseline, counts);

        Assertions.assertEquals(counts, PmdRatchet.readBaseline(baseline), "the file keeps every count");
        Assertions.assertTrue(Files.readString(baseline).contains(KEY + "=2"), "keys are written as path#rule=count");
    }

    @DisplayName("A missing baseline file is an empty baseline")
    @Test
    void aMissingBaselineIsEmpty() throws IOException {
        Assertions.assertTrue(PmdRatchet.readBaseline(root.resolve("absent.properties")).isEmpty(), "no file, no allowed violations");
    }

    private Path writeReport(String... lines) throws IOException {
        Path report = root.resolve("main.xml");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<pmd version=\"7.28.0\">\n" + String.join("\n", lines) + "\n</pmd>\n";
        Files.writeString(report, xml);
        return report;
    }
}
