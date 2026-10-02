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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelTextTest {

    private static final MiniMessage WITH_PREFIX = MiniMessage.builder().editTags(tags -> tags.resolver(Placeholder.parsed("prefix", "[T]"))).build();

    private static PortalLabel label(String text, String offlineText) {
        return new PortalLabel(new Vec(0, 64, 0), text, offlineText, null, Billboard.CENTER, 0f);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @DisplayName("Online shows the text with the given counts and task")
    @Test
    void onlineFillsPlaceholders() {
        assertEquals("Survival 12/50", plain(LabelText.render(label("<task> <online>/<max>", "closed"), "Survival", "12", "50", false)));
    }

    @DisplayName("Offline shows the offline text when there is one")
    @Test
    void offlineShowsOfflineText() {
        assertEquals("closed", plain(LabelText.render(label("<task> <online>/<max>", "closed"), "Survival", "0", "0", true)));
    }

    @DisplayName("Offline without an offline text shows the text with the given counts")
    @Test
    void offlineFallsBackToText() {
        assertEquals("Survival 0/0", plain(LabelText.render(label("<task> <online>/<max>", null), "Survival", "0", "0", true)));
    }

    @DisplayName("Tags inside the task stay literal")
    @Test
    void taskTagsStayLiteral() {
        assertEquals("<red>x", plain(LabelText.render(label("<task>", null), "<red>x", "1", "1", false)));
    }

    @DisplayName("The given parser resolves its own prefix")
    @Test
    void parserResolvesPrefix() {
        assertEquals("[T] 1", plain(LabelText.render(WITH_PREFIX, label("<prefix> <online>", null), "t", "1", "1", false)));
    }

    @DisplayName("textProblems names the field and the unknown tag")
    @Test
    void textProblemsNameFieldAndTag() {
        List<String> problems = PortalValidator.textProblems("label.text", "<gold>Survival</red>");

        assertEquals(1, problems.size(), "one problem: " + problems);
        assertTrue(problems.getFirst().startsWith("label.text: "), "starts with the field: " + problems.getFirst());
    }

    @DisplayName("textProblems accepts a valid text and rejects a blank one")
    @Test
    void textProblemsValidAndBlank() {
        assertTrue(PortalValidator.textProblems("label.text", "<gold><task> <online>/<max>").isEmpty(), "valid text");
        assertEquals(List.of("label.text must not be blank"), PortalValidator.textProblems("label.text", " "));
    }
}
