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
package net.onelitefeather.titan.core.portal;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalValidatorLabelTest {

    private static final Box BOX = new Box(new Vec(10, 64, 10), new Vec(14, 68, 11));
    private static final Vec ANCHOR = new Vec(12.5, 66, -3.5);

    private static PortalLabel label(String text) {
        return new PortalLabel(ANCHOR, text, null, null, Billboard.CENTER, 0);
    }

    private static PortalLabel withSource(@Nullable LabelSource source) {
        return new PortalLabel(ANCHOR, "<gray>Survival", null, source, Billboard.CENTER, 0);
    }

    private static Portal portal(PortalLabel label) {
        return new Portal("survival", BOX, "Survival", null, label);
    }

    private static List<PortalProblem> problems(PortalLabel label) {
        return PortalValidator.problems(List.of(portal(label)));
    }

    private static void assertSingleReasonContaining(List<PortalProblem> problems, String expected) {
        assertEquals(1, problems.size(), "exactly one problem expected but got " + problems);
        String reason = problems.getFirst().reason();
        assertTrue(reason.contains(expected), "reason must contain '" + expected + "' but was: " + reason);
    }

    @DisplayName("A portal without a label has no label problems")
    @Test
    void portalWithoutLabelIsValid() {
        assertTrue(PortalValidator.problems(List.of(new Portal("p", BOX, "Survival", null))).isEmpty());
    }

    @DisplayName("A label with a full valid text and every source type is valid")
    @Test
    void validLabelsAreAccepted() {
        for (LabelSource source : new LabelSource[]{null, new LabelSource.Task("T"), new LabelSource.Group("G"), new LabelSource.Service("S-1"), new LabelSource.Local()}) {
            List<PortalProblem> problems = problems(withSource(source));
            assertTrue(problems.isEmpty(), "source " + source + " must be valid but got " + problems);
        }
    }

    @DisplayName("online, max, task and prefix are valid placeholders")
    @ParameterizedTest
    @ValueSource(strings = {"<online>", "<max>", "<task>", "<prefix>", "<gold>Survival<newline><gray><online>/<max> <task>"})
    void placeholdersAreValid(String text) {
        assertTrue(problems(label(text)).isEmpty(), text + " must be valid");
    }

    @DisplayName("Literal angle brackets and escaped tags are plain text, not tags")
    @ParameterizedTest
    @ValueSource(strings = {"<3", "a < b", "I <3 you", "\\<gold>", "use \\<red> for red"})
    void literalBracketsAreValid(String text) {
        assertTrue(problems(label(text)).isEmpty(), text + " must be valid");
    }

    @DisplayName("An unescaped unknown tag next to an escaped one is still rejected")
    @Test
    void escapedTagDoesNotHideARealOne() {
        assertSingleReasonContaining(problems(label("\\<gold> <nope>")), "label.text");
    }

    @DisplayName("A wrongly closed tag in text names the field")
    @Test
    void wronglyClosedTag() {
        assertSingleReasonContaining(problems(label("<gold>Survival</red>")), "label.text");
    }

    @DisplayName("An unknown tag in text is rejected by the strict parser")
    @Test
    void unknownTag() {
        assertSingleReasonContaining(problems(label("<online:group:x>")), "label.text");
    }

    @DisplayName("An invalid hex colour is rejected like any other unknown tag")
    @Test
    void invalidHexColour() {
        assertSingleReasonContaining(problems(label("<#zzzzzz>x")), "label.text");
    }

    @DisplayName("A local source with a name present is still valid")
    @Test
    void localSourceIgnoresNothingElse() {
        assertTrue(problems(withSource(new LabelSource.Local())).isEmpty(), "local needs no name");
    }

    @DisplayName("A bad offlineText names its own field")
    @Test
    void badOfflineText() {
        PortalLabel label = new PortalLabel(ANCHOR, "ok", "<red>starting</gold>", null, Billboard.CENTER, 0);

        assertSingleReasonContaining(problems(label), "label.offlineText");
    }

    @DisplayName("A blank text is reported")
    @Test
    void blankText() {
        assertSingleReasonContaining(problems(label(" ")), "label.text");
    }

    @DisplayName("An unknown source type names the type")
    @Test
    void unknownSourceType() {
        List<PortalProblem> problems = problems(withSource(new LabelSource.Unknown("proxy")));

        assertSingleReasonContaining(problems, "label.source.type");
        assertTrue(problems.getFirst().reason().contains("proxy"), "reason must name the type: " + problems);
    }

    @DisplayName("A source without a type is reported")
    @Test
    void missingSourceType() {
        assertSingleReasonContaining(problems(withSource(new LabelSource.Unknown(null))), "label.source.type");
    }

    @DisplayName("task, group and service need a name")
    @Test
    void nameMissing() {
        for (LabelSource source : new LabelSource[]{new LabelSource.Task(null), new LabelSource.Group(" "), new LabelSource.Service("")}) {
            assertSingleReasonContaining(problems(withSource(source)), "label.source.name");
        }
    }

    @DisplayName("A missing position is reported")
    @Test
    void missingPosition() {
        PortalLabel label = new PortalLabel(null, "ok", null, null, Billboard.CENTER, 0);

        assertSingleReasonContaining(problems(label), "label.position");
    }

    @DisplayName("An unknown billboard is reported")
    @Test
    void unknownBillboard() {
        PortalLabel label = new PortalLabel(ANCHOR, "ok", null, null, Billboard.UNKNOWN, 0);

        assertSingleReasonContaining(problems(label), "label.billboard");
    }

    @DisplayName("requireValid aborts for an invalid label naming portal and field")
    @Test
    void requireValidAborts() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PortalValidator.requireValid("world", List.of(portal(label("<gold>Survival</red>")))));

        String message = failure.getMessage();
        assertTrue(message.contains("'survival'") && message.contains("label.text"), "message must name portal and field: " + message);
    }

    @DisplayName("A single source is checked on its own with the reasons the lobby reports")
    @Test
    void sourceProblemsNameTheReasons() {
        assertEquals(List.of(), PortalValidator.sourceProblems(null), "no source is fine");
        assertEquals(List.of(), PortalValidator.sourceProblems(new LabelSource.Local()), "local needs no name");
        assertEquals(List.of("label.source.name is missing for type 'task'"), PortalValidator.sourceProblems(new LabelSource.Task(" ")), "blank name");
        assertEquals(List.of("label.source.type 'proxy' is unknown (expected task, group, service or local)"), PortalValidator.sourceProblems(LabelSource.of("proxy", "x")), "unknown type");
    }
}
