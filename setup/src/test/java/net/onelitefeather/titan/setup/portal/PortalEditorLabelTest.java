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
package net.onelitefeather.titan.setup.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.PortalProblem;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Invalid;
import net.onelitefeather.titan.setup.portal.PortalEditResult.LabelUpdated;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Rejected;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Saved;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Updated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalEditorLabelTest {

    private static final UUID ALICE = new UUID(0, 1);
    private static final Vec ANCHOR = new Vec(12.5, 66.0, -3.5);
    private static final Box BOX = new Box(new Vec(10, 64, 10), new Vec(14, 68, 11));

    private InMemoryPortalStore store;
    private PortalEditor editor;

    @BeforeEach
    void freshFixture() {
        store = new InMemoryPortalStore();
        editor = new PortalEditor(store);
    }

    private void completeBox() {
        editor.corner1(ALICE, "survival", new Pos(10, 64, 10));
        editor.corner2(ALICE, "survival", new Pos(14, 68, 11));
        editor.task(ALICE, "survival", "Survival");
    }

    private PortalDraft draft() {
        return editor.draft(ALICE, "survival").orElseThrow();
    }

    @Test
    @DisplayName("label here keeps the exact position as anchor and saves nothing")
    void hereSetsTheAnchor() {
        PortalEditResult result = editor.labelHere(ALICE, "survival", new Pos(12.5, 66.0, -3.5));

        assertEquals(ANCHOR, draft().labelPosition(), "the anchor is the unrounded position");
        assertInstanceOf(LabelUpdated.class, result, "the answer states the draft");
        assertEquals(0, store.saves(), "nothing is saved");
    }

    @Test
    @DisplayName("label text keeps the whole text with its spaces")
    void textKeepsSpaces() {
        editor.labelText(ALICE, "survival", "<gold>Survival <gray><online>/<max>");

        assertEquals("<gold>Survival <gray><online>/<max>", draft().labelText(), "text as typed");
    }

    @Test
    @DisplayName("A blank label text is refused")
    void blankTextIsRefused() {
        assertInstanceOf(Invalid.class, editor.labelText(ALICE, "survival", "  "), "blank text refused");
        assertTrue(editor.drafts(ALICE).isEmpty(), "no draft was opened by the refused edit");
    }

    @Test
    @DisplayName("label offline sets the offline text")
    void offlineSetsTheText() {
        editor.labelOffline(ALICE, "survival", "<red>Survival startet gleich");

        assertEquals("<red>Survival startet gleich", draft().labelOffline(), "offline text remembered");
    }

    @Test
    @DisplayName("label source group takes type and name")
    void sourceGroup() {
        editor.labelSource(ALICE, "survival", "group", "Games");

        assertEquals(new LabelSource.Group("Games"), draft().labelSource(), "group source");
    }

    @Test
    @DisplayName("label source local needs no name")
    void sourceLocalNeedsNoName() {
        editor.labelSource(ALICE, "survival", "local", null);

        assertEquals(new LabelSource.Local(), draft().labelSource(), "local source");
    }

    @Test
    @DisplayName("Task and service sources take their names")
    void namedSourceTypes() {
        editor.labelSource(ALICE, "survival", "task", "T");
        assertEquals(new LabelSource.Task("T"), draft().labelSource(), "task source");
        editor.labelSource(ALICE, "survival", "service", "Lobby-1");
        assertEquals(new LabelSource.Service("Lobby-1"), draft().labelSource(), "service source");
    }

    @Test
    @DisplayName("label source task without a name is refused and the draft stays")
    void sourceWithoutNameIsRefused() {
        editor.labelSource(ALICE, "survival", "group", "Games");

        PortalEditResult result = editor.labelSource(ALICE, "survival", "task", null);

        assertInstanceOf(Invalid.class, result, "refused");
        assertEquals(new LabelSource.Group("Games"), draft().labelSource(), "draft unchanged");
    }

    @Test
    @DisplayName("An unknown source type is refused with the allowed types")
    void unknownTypeIsRefused() {
        PortalEditResult result = editor.labelSource(ALICE, "survival", "proxy", "x");

        Invalid invalid = assertInstanceOf(Invalid.class, result, "refused");
        assertTrue(invalid.reason().contains("task, group, service, local"), "names the types: " + invalid.reason());
    }

    @Test
    @DisplayName("A label with text but no anchor is incomplete and names the position")
    void labelWithoutPositionIsIncomplete() {
        completeBox();

        PortalEditResult result = editor.labelText(ALICE, "survival", "Hi");

        LabelUpdated updated = assertInstanceOf(LabelUpdated.class, result, "label answer");
        assertEquals(List.of(Missing.LABEL_POSITION), updated.missing(), "position missing");
        assertEquals(new PortalEditResult.Pending("survival", List.of(Missing.LABEL_POSITION)), editor.save(ALICE, "survival"), "save reports it too");
    }

    @Test
    @DisplayName("A label with an anchor but no text is incomplete and names the text")
    void labelWithoutTextIsIncomplete() {
        completeBox();

        LabelUpdated updated = assertInstanceOf(LabelUpdated.class, editor.labelHere(ALICE, "survival", new Pos(1, 2, 3)), "label answer");

        assertEquals(List.of(Missing.LABEL_TEXT), updated.missing(), "text missing");
    }

    @Test
    @DisplayName("save with a valid label writes it to the store")
    void saveWritesTheLabel() {
        completeBox();
        editor.labelHere(ALICE, "survival", new Pos(12.5, 66.0, -3.5));
        editor.labelText(ALICE, "survival", "<gold>Survival");
        editor.labelSource(ALICE, "survival", "group", "Games");

        PortalEditResult result = editor.save(ALICE, "survival");

        PortalLabel label = new PortalLabel(ANCHOR, "<gold>Survival", null, new LabelSource.Group("Games"), Billboard.CENTER, 0f);
        Portal expected = new Portal("survival", BOX, "Survival", null, label);
        assertEquals(new Saved(expected), result, "saved with label");
        assertEquals(List.of(expected), store.portals(), "label in the store");
    }

    @Test
    @DisplayName("save with an invalid label gives the validator's reasons and keeps draft and store")
    void saveRejectsInvalidLabel() {
        completeBox();
        editor.labelHere(ALICE, "survival", new Pos(1, 2, 3));
        editor.labelText(ALICE, "survival", "<gold>Survival</red>");

        PortalEditResult result = editor.save(ALICE, "survival");

        Rejected rejected = assertInstanceOf(Rejected.class, result, "rejected");
        assertTrue(rejected.problems().stream().map(PortalProblem::reason).anyMatch(reason -> reason.startsWith("label.text")), "reason names the field: " + rejected.problems());
        assertEquals(0, store.saves(), "store untouched");
        assertEquals("<gold>Survival</red>", draft().labelText(), "draft stays open");
    }

    @Test
    @DisplayName("A label change on a saved portal only reaches the store with save")
    void changeOnSavedPortalNeedsSave() {
        PortalLabel old = new PortalLabel(ANCHOR, "Old", null, null, Billboard.CENTER, 0f);
        store = new InMemoryPortalStore(new Portal("survival", BOX, "Survival", null, old));
        editor = new PortalEditor(store);

        editor.labelText(ALICE, "survival", "<red>Neu");

        assertEquals("Old", store.portals().getFirst().label().text(), "store still has the old text");
        assertInstanceOf(Updated.class, editor.save(ALICE, "survival"), "save updates");
        assertEquals("<red>Neu", store.portals().getFirst().label().text(), "new text after save");
        assertEquals(ANCHOR, store.portals().getFirst().label().position(), "anchor kept");
    }

    @Test
    @DisplayName("label remove drops the label from the saved portal on save")
    void removeDropsTheLabel() {
        PortalLabel old = new PortalLabel(ANCHOR, "Old", null, null, Billboard.FIXED, 90f);
        store = new InMemoryPortalStore(new Portal("survival", BOX, "Survival", null, old));
        editor = new PortalEditor(store);

        editor.labelRemove(ALICE, "survival");
        editor.save(ALICE, "survival");

        assertNull(store.portals().getFirst().label(), "no label after save");
    }

    @Test
    @DisplayName("Billboard and yaw of a saved label survive editing the text")
    void billboardSurvivesEdits() {
        PortalLabel old = new PortalLabel(ANCHOR, "Old", null, null, Billboard.FIXED, 90f);
        store = new InMemoryPortalStore(new Portal("survival", BOX, "Survival", null, old));
        editor = new PortalEditor(store);

        editor.labelText(ALICE, "survival", "New");
        editor.save(ALICE, "survival");

        PortalLabel saved = store.portals().getFirst().label();
        assertEquals(Billboard.FIXED, saved.billboard(), "billboard kept");
        assertEquals(90f, saved.yaw(), "yaw kept");
    }

    @Test
    @DisplayName("A portal without a label saves without one")
    void portalWithoutLabel() {
        completeBox();

        editor.save(ALICE, "survival");

        assertNull(store.portals().getFirst().label(), "no label");
    }

    @Test
    @DisplayName("An invalid id is refused by every label verb")
    void invalidIdIsRefused() {
        assertInstanceOf(Invalid.class, editor.labelHere(ALICE, "Bad Id", new Pos(0, 0, 0)), "here");
        assertInstanceOf(Invalid.class, editor.labelText(ALICE, "Bad Id", "x"), "text");
        assertInstanceOf(Invalid.class, editor.labelRemove(ALICE, "Bad Id"), "remove");
    }
}
