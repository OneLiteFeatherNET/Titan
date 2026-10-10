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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.Copied;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.NothingToCopy;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.SameWorld;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.UnknownWorld;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.Unreadable;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Pending;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Saved;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalMessagesCopyTest {

    private static final Portal SURVIVAL = new Portal("survival", new Box(new Vec(10, 64, 10), new Vec(14, 68, 11)), "Survival", null);
    private static final Portal CREATIVE = new Portal("creative", new Box(new Vec(20, 64, 20), new Vec(24, 68, 21)), "Creative", null);

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Every run-command click on the message, as the event it carries. */
    private static List<ClickEvent> commands(Component component) {
        List<ClickEvent> found = new ArrayList<>();
        ClickEvent click = component.clickEvent();
        if (click != null && click.action() == ClickEvent.Action.RUN_COMMAND) {
            found.add(click);
        }
        component.children().forEach(child -> found.addAll(commands(child)));
        return found;
    }

    private static String line(String text, String needle) {
        return text.lines().filter(candidate -> candidate.contains(needle)).findFirst().orElse("");
    }

    @Test
    @DisplayName("The copy names the added ids and the replaced ids on separate lines")
    void copyNamesAddedAndReplacedSeparately() {
        Copied copied = new Copied("lobby", List.of(SURVIVAL, CREATIVE), List.of("creative"), List.of("survival"), List.of());

        String text = plain(PortalMessages.copy(copied));

        assertTrue(line(text, "Replaced").contains("survival"), "the replaced id is named: " + text);
        assertFalse(line(text, "Replaced").contains("creative"), "a new id is not called replaced: " + text);
        assertTrue(line(text, "Added").contains("creative"), "the new id is named: " + text);
    }

    @Test
    @DisplayName("The copy offers a clickable save all when drafts were opened")
    void copyOffersSaveAll() {
        Copied copied = new Copied("lobby", List.of(SURVIVAL), List.of("survival"), List.of(), List.of());

        assertEquals(List.of(ClickEvent.runCommand("/setup portal save-all")), commands(PortalMessages.copy(copied)), "the button runs save-all");
    }

    @Test
    @DisplayName("A copy that opened no drafts offers no save all")
    void copyWithoutDraftsOffersNothing() {
        Copied copied = new Copied("lobby", List.of(), List.of(), List.of(), List.of(new SkippedPortal("survival", "reason")));

        assertEquals(List.of(), commands(PortalMessages.copy(copied)), "there is nothing to save");
    }

    @Test
    @DisplayName("Skipped ids are named with their reason")
    void copyNamesSkippedIdsWithReason() {
        Copied copied = new Copied("lobby", List.of(CREATIVE), List.of("creative"), List.of(), List.of(new SkippedPortal("survival", "you already have an open draft of this id")));

        String text = plain(PortalMessages.copy(copied));

        assertTrue(line(text, "Skipped").contains("survival") && line(text, "Skipped").contains("open draft"), "the skipped id comes with its reason: " + text);
    }

    @Test
    @DisplayName("An unknown world is named together with the worlds to choose from")
    void unknownWorldNamesTheAvailable() {
        String text = plain(PortalMessages.copy(new UnknownWorld("nirgendwo", List.of("lobby", "summer"))));

        assertTrue(text.contains("nirgendwo") && text.contains("lobby") && text.contains("summer"), "name and choices are shown: " + text);
    }

    @Test
    @DisplayName("Copying the loaded world says it is the loaded world")
    void sameWorldIsSaidPlainly() {
        String text = plain(PortalMessages.copy(new SameWorld("winter")));

        assertTrue(text.contains("winter") && text.contains("loaded"), "the loaded world is named: " + text);
    }

    @Test
    @DisplayName("A source without portals is said to have nothing to copy")
    void nothingToCopyIsSaidPlainly() {
        String text = plain(PortalMessages.copy(new NothingToCopy("empty")));

        assertTrue(text.contains("empty") && text.contains("no portals to copy"), "the source is named: " + text);
    }

    @Test
    @DisplayName("An unreadable source is said to be unreadable with the reason")
    void unreadableSourceGivesTheReason() {
        String text = plain(PortalMessages.copy(new Unreadable("broken", "map.json is not valid JSON")));

        assertTrue(text.contains("broken") && text.contains("map.json is not valid JSON"), "the reason is shown: " + text);
    }

    @Test
    @DisplayName("Save all answers each draft on its own line")
    void saveAllAnswersEachDraft() {
        SaveAllResult result = new SaveAllResult(List.of(new Saved(SURVIVAL), new Pending("half", List.of(Missing.TASK))));

        String text = plain(PortalMessages.saveAll(result));

        assertTrue(text.contains("Saved portal survival"), "the saved id is named: " + text);
        assertTrue(text.contains("Portal half is not complete yet, missing: task"), "the open draft says what is missing: " + text);
    }

    @Test
    @DisplayName("Save all without drafts says there is nothing to save")
    void saveAllWithoutDraftsSaysSo() {
        String text = plain(PortalMessages.saveAll(new SaveAllResult(List.of())));

        assertTrue(text.contains("no open drafts"), "the empty answer is explicit: " + text);
    }
}
