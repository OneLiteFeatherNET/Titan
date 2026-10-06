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
package net.onelitefeather.titan.setup.portal.editor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.LabelUpdated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalMessagesLabelTest {

    private static final String CLICK_TEXT = "<click:run_command:/op x>Hi";

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static boolean hasClick(Component component) {
        return component.clickEvent() != null || component.children().stream().anyMatch(PortalMessagesLabelTest::hasClick);
    }

    @Test
    @DisplayName("The preview problem names the reason and shows tags literally")
    void previewProblemIsLiteral() {
        Component message = PortalMessages.previewProblem("label.text: unknown or mismatched tag </red>");

        assertTrue(plain(message).contains("unknown or mismatched tag </red>"), plain(message));
        assertFalse(hasClick(PortalMessages.previewProblem(CLICK_TEXT)), "no click event from the reason");
    }

    @Test
    @DisplayName("A label answer states position, text, offline text and source")
    void labelAnswerStatesTheDraft() {
        Component message = PortalMessages.render(new LabelUpdated("survival", label(new Vec(12.5, 66, -3.5), "<gold>Survival", "<red>Soon", new LabelSource.Group("Games")), List.of()));

        String text = plain(message);
        assertTrue(text.contains("survival"), text);
        assertTrue(text.contains("(12.5, 66, -3.5)"), text);
        assertTrue(text.contains("<gold>Survival"), "the text is shown literally: " + text);
        assertTrue(text.contains("<red>Soon"), text);
        assertTrue(text.contains("group Games"), text);
    }

    @Test
    @DisplayName("Tags in the label text stay literal and carry no click event")
    void tagsStayLiteral() {
        Component message = PortalMessages.render(new LabelUpdated("survival", label(null, CLICK_TEXT, null, null), List.of(Missing.LABEL_POSITION)));

        assertTrue(plain(message).contains(CLICK_TEXT), "literal text: " + plain(message));
        assertFalse(hasClick(message), "nothing typed becomes a click event");
    }

    @Test
    @DisplayName("Tags in the label text stay literal when a saved portal is shown")
    void savedPortalShowsLabelLiterally() {
        PortalLabel label = new PortalLabel(new Vec(1, 2, 3), CLICK_TEXT, null, null, Billboard.CENTER, 0f);
        Portal portal = new Portal("survival", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "Survival", null, label);

        Component message = PortalMessages.render(new PortalEditResult.Saved(portal));

        assertTrue(plain(message).contains(CLICK_TEXT), "literal text: " + plain(message));
        assertFalse(hasClick(message), "no click event");
    }

    @Test
    @DisplayName("An incomplete label names what is missing")
    void incompleteLabelNamesTheMissingPart() {
        String text = plain(PortalMessages.render(new LabelUpdated("survival", label(null, "Hi", null, null), List.of(Missing.LABEL_POSITION))));

        assertTrue(text.contains("label position"), text);
    }

    @Test
    @DisplayName("A draft without a label says so")
    void removedLabelIsAnnounced() {
        String text = plain(PortalMessages.render(new LabelUpdated("survival", label(null, null, null, null), List.of())));

        assertTrue(text.contains("no label"), text);
    }

    @Test
    @DisplayName("Every missing part, label parts included, is described")
    void everyMissingPartIsDescribed() {
        for (Missing missing : Missing.values()) {
            String text = plain(PortalMessages.render(new PortalEditResult.Pending("a", List.of(missing))));
            assertTrue(text.contains("missing: ") && !text.endsWith("missing: ."), missing + ": " + text);
        }
    }

    @Test
    @DisplayName("The usage mentions the label commands")
    void usageMentionsLabels() {
        String usage = plain(PortalMessages.usage());

        assertTrue(usage.contains("label here|text <mm>|offline <mm>|source <type> [name]|remove"), usage);
    }

    private static LabelDraft label(Vec position, String text, String offline, LabelSource source) {
        return LabelDraft.of(position == null && text == null && offline == null && source == null ? null : new PortalLabel(position, text, offline, source, Billboard.CENTER, 0f));
    }
}
