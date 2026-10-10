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

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.InMemoryPortalStore;
import net.onelitefeather.titan.setup.portal.PortalStore;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Pending;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Rejected;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Saved;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Updated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalEditorSaveAllTest {

    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);

    private InMemoryPortalStore store;
    private PortalEditor editor;

    @BeforeEach
    void freshFixture() {
        store = new InMemoryPortalStore();
        editor = new PortalEditor(store);
    }

    private static Box box(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Box(new Vec(x1, y1, z1), new Vec(x2, y2, z2));
    }

    private void completeBox(UUID player, String id) {
        editor.corner1(player, id, new Pos(10, 64, 10));
        editor.corner2(player, id, new Pos(14, 68, 11));
        editor.task(player, id, "Survival");
    }

    /** Two complete drafts around one incomplete one, in this order. */
    private static void prepareSameDrafts(PortalEditor target) {
        target.corner1(ALICE, "a", new Pos(10, 64, 10));
        target.corner2(ALICE, "a", new Pos(14, 68, 11));
        target.task(ALICE, "a", "Survival");
        target.corner1(ALICE, "c", new Pos(0, 0, 0));
        target.corner1(ALICE, "b", new Pos(10, 64, 10));
        target.corner2(ALICE, "b", new Pos(14, 68, 11));
        target.task(ALICE, "b", "Creative");
    }

    private static List<String> ids(List<Portal> portals) {
        return portals.stream().map(Portal::id).toList();
    }

    private List<String> openDraftIds(UUID player) {
        return editor.drafts(player).stream().map(PortalDraft::id).toList();
    }

    @Test
    @DisplayName("Save all writes every complete draft of the player in one store write")
    void savesEveryCompleteDraftInOneWrite() {
        completeBox(ALICE, "a");
        completeBox(ALICE, "b");

        SaveAllResult result = editor.saveAll(ALICE);

        assertEquals(List.of("a", "b"), ids(store.portals()), "both drafts must be in the map in draft order");
        assertEquals(1, store.saves(), "the map must be written exactly once");
        assertInstanceOf(Saved.class, result.results().get(0), "the first draft is reported as saved");
        assertInstanceOf(Saved.class, result.results().get(1), "the second draft is reported as saved");
        assertTrue(editor.drafts(ALICE).isEmpty(), "saved drafts are gone");
    }

    @Test
    @DisplayName("A rejected draft stays open while the valid drafts are saved")
    void rejectedDraftStaysOpenAndDoesNotBlockTheOthers() {
        Portal broken = new Portal("ring", new Disc(new Vec(0, 70, 0), 3, Vec.ZERO), "Survival", null);
        store = new InMemoryPortalStore(broken);
        editor = new PortalEditor(store);
        editor.permission(ALICE, "ring", "some.permission");
        completeBox(ALICE, "a");

        SaveAllResult result = editor.saveAll(ALICE);

        Rejected rejected = assertInstanceOf(Rejected.class, result.results().get(0), "the rejected draft is reported with its reason");
        assertEquals("ring", rejected.id(), "the rejection names the draft's id");
        assertEquals(List.of("normal must not have length 0"), rejected.problems().stream().map(problem -> problem.reason()).toList(), "the reason comes from the validator");
        assertInstanceOf(Saved.class, result.results().get(1), "the valid draft is still saved");
        assertEquals(List.of("ring", "a"), ids(store.portals()), "the broken portal stays, the valid one is appended");
        assertEquals(List.of("ring"), openDraftIds(ALICE), "only the rejected draft stays open");
    }

    @Test
    @DisplayName("An incomplete draft is reported with what is missing and stays open")
    void incompleteDraftStaysOpen() {
        editor.corner1(ALICE, "half", new Pos(0, 0, 0));
        completeBox(ALICE, "a");

        SaveAllResult result = editor.saveAll(ALICE);

        assertEquals(new Pending("half", List.of(Missing.CORNER_2, Missing.TASK)), result.results().get(0), "the missing parts are named");
        assertEquals(List.of("a"), ids(store.portals()), "only the complete draft is written");
        assertEquals(List.of("half"), openDraftIds(ALICE), "the incomplete draft stays open");
    }

    @Test
    @DisplayName("Save all replaces a saved portal with the same id at its position")
    void replacesSameIdInPlace() {
        Portal first = new Portal("a", box(0, 0, 0, 1, 1, 1), "First", null);
        Portal other = new Portal("b", box(5, 5, 5, 6, 6, 6), "Other", null);
        store = new InMemoryPortalStore(first, other);
        editor = new PortalEditor(store);
        editor.task(ALICE, "a", "Changed");

        SaveAllResult result = editor.saveAll(ALICE);

        Portal replaced = new Portal("a", box(0, 0, 0, 1, 1, 1), "Changed", null);
        assertEquals(new Updated(replaced), result.results().get(0), "an existing id is reported as updated");
        assertEquals(List.of(replaced, other), store.portals(), "replaced at the same position, no duplicate");
    }

    @Test
    @DisplayName("Save all without drafts reports nothing and writes nothing")
    void withoutDraftsNothingIsWritten() {
        SaveAllResult result = editor.saveAll(ALICE);

        assertEquals(List.of(), result.results(), "there is nothing to report");
        assertEquals(0, store.saves(), "the map is not written");
    }

    @Test
    @DisplayName("Save all leaves the drafts of other players alone")
    void otherPlayersDraftsStayOpen() {
        completeBox(BOB, "b");
        completeBox(ALICE, "a");

        editor.saveAll(ALICE);

        assertEquals(List.of("a"), ids(store.portals()), "only the first player's draft is saved");
        assertEquals(List.of("b"), openDraftIds(BOB), "the other player's draft stays open");
    }

    @Test
    @DisplayName("Save all gives the same results and map as saving each draft in turn")
    void sameResultsAsSavingEachDraftInTurn() {
        InMemoryPortalStore sequentialStore = new InMemoryPortalStore();
        PortalEditor sequential = new PortalEditor(sequentialStore);
        prepareSameDrafts(sequential);
        prepareSameDrafts(editor);
        List<PortalEditResult> sequentialResults = new ArrayList<>();
        for (String id : List.of("a", "c", "b")) {
            sequentialResults.add(sequential.save(ALICE, id));
        }

        SaveAllResult batch = editor.saveAll(ALICE);

        assertEquals(sequentialResults, batch.results(), "each draft is reported as saving it alone would");
        assertEquals(sequentialStore.portals(), store.portals(), "the map ends up identical");
    }

    @Test
    @DisplayName("A failing write keeps every draft open")
    void failingWriteKeepsDraftsOpen() {
        PortalStore failing = new PortalStore() {
            @Override
            public List<Portal> portals() {
                return List.of();
            }

            @Override
            public void save(List<Portal> portals) {
                throw new IllegalStateException("disk full");
            }
        };
        editor = new PortalEditor(failing);
        completeBox(ALICE, "a");
        completeBox(ALICE, "b");

        assertThrows(IllegalStateException.class, () -> editor.saveAll(ALICE), "the failure reaches the caller");

        assertEquals(List.of("a", "b"), openDraftIds(ALICE), "no draft is dropped when the write fails");
    }
}
