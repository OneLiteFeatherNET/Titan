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
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalProblem;
import net.onelitefeather.titan.setup.portal.InMemoryPortalStore;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Cancelled;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Complete;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Invalid;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Pending;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Rejected;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Removed;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Saved;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Unknown;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Updated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalMessagesTest {

    private static final Portal BOX_PORTAL = new Portal("survival", new Box(new Vec(10, 64, 10), new Vec(14, 68, 11)), "Survival", null);
    private static final Portal RING_PORTAL = new Portal("elytra-ring", new Disc(new Vec(0.5, 72, 40.5), 5.5, new Vec(0, 0, 1)), "Elytra", "titan.portal.elytra");

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String text(PortalEditResult result) {
        return plain(PortalMessages.render(result));
    }

    @Test
    @DisplayName("Saved names the portal, its span and its task")
    void savedBox() {
        String text = text(new Saved(BOX_PORTAL));

        assertTrue(text.contains("Saved portal survival"), text);
        assertTrue(text.contains("(10, 64, 10) to (14, 68, 11)"), text);
        assertTrue(text.contains("Survival"), text);
    }

    @Test
    @DisplayName("Saved ring names the stored centre, radius and normal")
    void savedRingShowsCentreAndNormal() {
        String text = text(new Saved(RING_PORTAL));

        assertTrue(text.contains("centre (0.5, 72, 40.5)"), text);
        assertTrue(text.contains("radius 5.5"), text);
        assertTrue(text.contains("normal (0, 0, 1)"), text);
        assertTrue(text.contains("titan.portal.elytra"), text);
    }

    @Test
    @DisplayName("Updated says the portal was updated")
    void updated() {
        assertTrue(text(new Updated(BOX_PORTAL)).contains("Updated portal survival"), "update wording");
    }

    @Test
    @DisplayName("Pending names every missing part")
    void pendingListsMissingParts() {
        String text = text(new Pending("survival", List.of(Missing.CORNER_2, Missing.TASK)));

        assertTrue(text.contains("survival"), text);
        assertTrue(text.contains("second corner"), text);
        assertTrue(text.contains("task"), text);
    }

    @Test
    @DisplayName("Every missing part has its own wording")
    void everyMissingPartIsDescribed() {
        for (Missing missing : Missing.values()) {
            String text = text(new Pending("x", List.of(missing)));

            assertTrue(text.length() > "x".length() + 20, "missing " + missing + " has words: " + text);
        }
    }

    @Test
    @DisplayName("Complete offers a clickable save button that runs the save command")
    void completeHasSaveButton() {
        Component message = PortalMessages.render(new Complete("survival"));

        assertTrue(plain(message).contains("complete"), plain(message));
        assertTrue(plain(message).contains("[save]"), plain(message));
        ClickEvent click = message.children().stream().map(Component::clickEvent).filter(Objects::nonNull).findFirst().orElseThrow(() -> new AssertionError("no clickable child"));
        assertEquals(ClickEvent.runCommand("/setup portal survival save"), click, "the button runs the ordinary save command");
    }

    @Test
    @DisplayName("Removed, cancelled and unknown name the id")
    void removedCancelledUnknown() {
        assertTrue(text(new Removed("survival")).contains("Removed portal survival"), "removed");
        assertTrue(text(new Cancelled("survival")).contains("survival"), "cancelled");
        assertTrue(text(new Unknown("survival")).contains("survival"), "unknown");
    }

    @Test
    @DisplayName("Invalid shows the reason")
    void invalidShowsReason() {
        assertTrue(text(new Invalid("the radius must be a number greater than 0")).contains("The radius must be a number greater than 0"), "reason is shown, capitalised");
    }

    @Test
    @DisplayName("Rejected lists each problem as portal label plus reason")
    void rejectedMapsProblems() {
        PortalProblem named = new PortalProblem("ring", 1, "normal must not have length 0");
        PortalProblem unnamed = new PortalProblem(" ", 3, "task must not be blank");

        String text = text(new Rejected("ring", List.of(named, unnamed)));

        assertTrue(text.contains(named.portalLabel() + ": normal must not have length 0"), text);
        assertTrue(text.contains(unnamed.portalLabel() + ": task must not be blank"), text);
    }

    @Test
    @DisplayName("MiniMessage tags in a task are shown literally")
    void taskTagsAreNotParsed() {
        Portal tricky = new Portal("survival", BOX_PORTAL.shape(), "<red>Sur</red><click:run_command:'/op me'>", "<b>perm");

        String text = text(new Saved(tricky));

        assertTrue(text.contains("<red>Sur</red><click:run_command:'/op me'>"), text);
        assertTrue(text.contains("<b>perm"), text);
    }

    @Test
    @DisplayName("MiniMessage tags in a problem's id and reason are shown literally")
    void problemTagsAreNotParsed() {
        PortalProblem tricky = new PortalProblem("<b>x", 0, "reason <i>y");

        String text = text(new Rejected("x", List.of(tricky)));

        assertTrue(text.contains("portal '<b>x': reason <i>y"), text);
    }

    @Test
    @DisplayName("Invalid reason with tags is shown literally")
    void invalidReasonTagsAreNotParsed() {
        assertTrue(text(new Invalid("<red>oops")).contains("<red>oops"), "reason is unparsed");
    }

    @Test
    @DisplayName("List shows saved portals and, separately, the drafts with their state")
    void listShowsPortalsAndDrafts() {
        PortalEditor editor = new PortalEditor(new InMemoryPortalStore());
        UUID player = new UUID(0, 1);
        editor.corner1(player, "arena", new Pos(0, 0, 0));
        editor.corner2(player, "arena", new Pos(1, 1, 1));
        editor.corner1(player, "done", new Pos(0, 0, 0));
        editor.corner2(player, "done", new Pos(1, 1, 1));
        editor.task(player, "done", "Done");

        String text = plain(PortalMessages.list(List.of(BOX_PORTAL, RING_PORTAL), editor.drafts(player)));

        assertTrue(text.contains("survival"), text);
        assertTrue(text.contains("elytra-ring"), text);
        assertTrue(text.contains("Drafts"), text);
        assertTrue(text.indexOf("Drafts") > text.indexOf("elytra-ring"), "drafts come after the portals: " + text);
        assertTrue(text.contains("arena") && text.contains("task"), text);
        assertTrue(text.contains("complete, not saved"), text);
    }

    @Test
    @DisplayName("List of nothing says there are none")
    void emptyList() {
        String text = plain(PortalMessages.list(List.of(), List.of()));

        assertTrue(text.contains("no portals"), text);
    }

    @Test
    @DisplayName("Every result type renders a non-empty message")
    void everyResultRenders() {
        List<PortalEditResult> results = List.of(new Saved(BOX_PORTAL), new Updated(BOX_PORTAL), new Pending("a", List.of(Missing.TASK)), new Complete("a"), new Removed("a"), new Rejected("a", List.of(new PortalProblem("a", 0, "r"))), new Invalid("r"), new Cancelled("a"), new Unknown("a"));
        for (PortalEditResult result : results) {
            assertInstanceOf(Component.class, PortalMessages.render(result), result + " renders");
            assertTrue(!text(result).isBlank(), result + " is not blank");
        }
    }
}
