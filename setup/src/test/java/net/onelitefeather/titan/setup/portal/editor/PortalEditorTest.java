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
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Cancelled;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Complete;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Invalid;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Pending;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Rejected;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Removed;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Saved;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Unknown;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Updated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalEditorTest {

    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);
    private static final Vec LOOK_Z = new Vec(0, 0, 1);

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

    // --- drafts and explicit save ---------------------------------------------------------

    @Test
    @DisplayName("A first corner alone reports the second corner and the task as missing")
    void firstCornerReportsWhatIsMissing() {
        PortalEditResult result = editor.corner1(ALICE, "survival", new Pos(1, 2, 3));

        assertEquals(new Pending("survival", List.of(Missing.CORNER_2, Missing.TASK)), result, "pending lists the missing parts");
        assertTrue(store.portals().isEmpty(), "nothing is saved");
    }

    @Test
    @DisplayName("A complete draft is reported as complete but not saved")
    void completeDraftIsNotSavedWithoutSave() {
        editor.corner1(ALICE, "survival", new Pos(10, 64, 10));
        editor.corner2(ALICE, "survival", new Pos(14, 68, 11));

        PortalEditResult result = editor.task(ALICE, "survival", "Survival");

        assertEquals(new Complete("survival"), result, "complete draft gets the save hint");
        assertEquals(0, store.saves(), "the store was never written");
        assertTrue(store.portals().isEmpty(), "the map stays unchanged");
    }

    @Test
    @DisplayName("Save persists the complete draft")
    void saveWritesThePortal() {
        completeBox(ALICE, "survival");

        PortalEditResult result = editor.save(ALICE, "survival");

        Portal expected = new Portal("survival", box(10, 64, 10, 14, 68, 11), "Survival", null);
        assertEquals(new Saved(expected), result, "a new id is reported as saved");
        assertEquals(List.of(expected), store.portals(), "the portal is in the store");
        assertTrue(editor.drafts(ALICE).isEmpty(), "the draft is gone after saving");
    }

    @Test
    @DisplayName("Save replaces a portal with the same id in place without a duplicate")
    void saveReplacesSameId() {
        Portal first = new Portal("a", box(0, 0, 0, 1, 1, 1), "First", null);
        Portal other = new Portal("b", box(5, 5, 5, 6, 6, 6), "Other", null);
        store = new InMemoryPortalStore(first, other);
        editor = new PortalEditor(store);
        editor.task(ALICE, "a", "Changed");

        PortalEditResult result = editor.save(ALICE, "a");

        Portal replaced = new Portal("a", box(0, 0, 0, 1, 1, 1), "Changed", null);
        assertEquals(new Updated(replaced), result, "an existing id is reported as updated");
        assertEquals(List.of(replaced, other), store.portals(), "replaced at the same position, no duplicate");
    }

    @Test
    @DisplayName("Save without a task reports what is missing and keeps the draft")
    void saveIncompleteKeepsDraft() {
        editor.corner1(ALICE, "survival", new Pos(0, 0, 0));
        editor.corner2(ALICE, "survival", new Pos(1, 1, 1));

        PortalEditResult result = editor.save(ALICE, "survival");

        assertEquals(new Pending("survival", List.of(Missing.TASK)), result, "the missing task is named");
        assertEquals(1, editor.drafts(ALICE).size(), "the draft stays open");
        assertEquals(0, store.saves(), "nothing is written");
    }

    @Test
    @DisplayName("A zero-length stored normal is rejected with the validator's reason")
    void zeroNormalIsRejected() {
        Portal broken = new Portal("ring", new Disc(new Vec(0, 70, 0), 3, Vec.ZERO), "Survival", null);
        store = new InMemoryPortalStore(broken);
        editor = new PortalEditor(store);
        editor.permission(ALICE, "ring", "some.permission");

        PortalEditResult result = editor.save(ALICE, "ring");

        Rejected rejected = assertInstanceOf(Rejected.class, result, "the validator's problem blocks the save");
        assertEquals("ring", rejected.id(), "the rejected id is named");
        assertEquals(List.of("normal must not have length 0"), rejected.problems().stream().map(p -> p.reason()).toList(), "reason comes from the validator");
        assertEquals(List.of(broken), store.portals(), "the map is unchanged");
        assertEquals(1, editor.drafts(ALICE).size(), "the draft stays open");
    }

    @Test
    @DisplayName("Problems of other portals do not block saving this one")
    void otherPortalsProblemsDoNotBlock() {
        Portal broken = new Portal("bad", box(5, 5, 5, 1, 1, 1), "Task", null);
        store = new InMemoryPortalStore(broken);
        editor = new PortalEditor(store);
        completeBox(ALICE, "good");

        PortalEditResult result = editor.save(ALICE, "good");

        assertInstanceOf(Saved.class, result, "only the edited portal's problems count");
        assertEquals(2, store.portals().size(), "the new portal is appended after the broken one");
    }

    @Test
    @DisplayName("Editing a saved portal stays a draft until save")
    void editOfSavedPortalStaysDraft() {
        Portal saved = new Portal("survival", box(0, 0, 0, 1, 1, 1), "Survival", null);
        store = new InMemoryPortalStore(saved);
        editor = new PortalEditor(store);

        PortalEditResult result = editor.permission(ALICE, "survival", "titan.portal.survival");

        assertEquals(new Complete("survival"), result, "the prefilled draft is complete");
        assertEquals(List.of(saved), store.portals(), "the map still has the old permission");
        editor.save(ALICE, "survival");
        assertEquals("titan.portal.survival", store.portals().getFirst().permission(), "save writes the new permission");
    }

    @Test
    @DisplayName("Cancel discards the draft and leaves the map alone")
    void cancelDiscardsDraft() {
        editor.corner1(ALICE, "survival", new Pos(0, 0, 0));

        PortalEditResult result = editor.cancel(ALICE, "survival");

        assertEquals(new Cancelled("survival"), result, "cancel is acknowledged");
        assertTrue(editor.drafts(ALICE).isEmpty(), "the draft is gone");
        assertEquals(0, store.saves(), "the map was not written");
    }

    @Test
    @DisplayName("Cancel without a draft is unknown")
    void cancelWithoutDraftIsUnknown() {
        assertEquals(new Unknown("survival"), editor.cancel(ALICE, "survival"), "nothing to cancel");
    }

    @Test
    @DisplayName("Cancel of a saved portal's draft keeps the saved portal")
    void cancelKeepsSavedPortal() {
        Portal saved = new Portal("survival", box(0, 0, 0, 1, 1, 1), "Survival", null);
        store = new InMemoryPortalStore(saved);
        editor = new PortalEditor(store);
        editor.task(ALICE, "survival", "Changed");

        editor.cancel(ALICE, "survival");

        assertEquals(List.of(saved), store.portals(), "the saved portal is unchanged");
    }

    @Test
    @DisplayName("Two players have separate drafts for the same id")
    void draftsArePerPlayer() {
        editor.corner1(ALICE, "survival", new Pos(0, 0, 0));
        editor.corner2(BOB, "survival", new Pos(9, 9, 9));

        assertEquals(List.of(Missing.CORNER_2, Missing.TASK), editor.drafts(ALICE).getFirst().missing(), "Alice still lacks corner 2");
        assertEquals(List.of(Missing.CORNER_1, Missing.TASK), editor.drafts(BOB).getFirst().missing(), "Bob still lacks corner 1");
        assertInstanceOf(Pending.class, editor.save(ALICE, "survival"), "corners of two players never form a box");
        assertTrue(store.portals().isEmpty(), "nothing is saved");
    }

    @Test
    @DisplayName("Discarding all drafts of a player leaves other players' drafts")
    void discardAllIsPerPlayer() {
        editor.corner1(ALICE, "a", new Pos(0, 0, 0));
        editor.corner1(BOB, "b", new Pos(0, 0, 0));

        editor.discardAll(ALICE);

        assertTrue(editor.drafts(ALICE).isEmpty(), "Alice's drafts are gone");
        assertEquals(1, editor.drafts(BOB).size(), "Bob keeps his draft");
    }

    // --- ids ------------------------------------------------------------------------------

    @Test
    @DisplayName("Reserved ids are rejected")
    void reservedIdsAreInvalid() {
        for (String id : List.of("list", "show", "create")) {
            assertInstanceOf(Invalid.class, editor.corner1(ALICE, id, new Pos(0, 0, 0)), "'" + id + "' is reserved");
        }
        assertTrue(editor.drafts(ALICE).isEmpty(), "no draft is created for a rejected id");
    }

    @Test
    @DisplayName("Ids outside [a-z0-9_-]+ are rejected")
    void badIdsAreInvalid() {
        for (String id : List.of("Survival!", "Survival", "with space", "", "a.b", "\u00e4")) {
            assertInstanceOf(Invalid.class, editor.corner1(ALICE, id, new Pos(0, 0, 0)), "'" + id + "' does not match the pattern");
            assertInstanceOf(Invalid.class, editor.save(ALICE, id), "save checks the id as well");
            assertInstanceOf(Invalid.class, editor.remove(ALICE, id), "remove checks the id as well");
        }
    }

    @Test
    @DisplayName("Ids from lowercase letters, digits, dash and underscore are accepted")
    void goodIdsAreAccepted() {
        for (String id : List.of("survival", "elytra-ring", "a_b", "x1", "-", "_")) {
            assertInstanceOf(Pending.class, editor.corner1(ALICE, id, new Pos(0, 0, 0)), "'" + id + "' is a valid id");
        }
    }

    // --- box ------------------------------------------------------------------------------

    @Test
    @DisplayName("Two corners in any order form the inclusive span")
    void cornersFormInclusiveSpanFromAnyOrder() {
        editor.corner1(ALICE, "survival", new Pos(14, 68, 11));
        editor.corner2(ALICE, "survival", new Pos(10, 64, 10));
        editor.task(ALICE, "survival", "Survival");

        editor.save(ALICE, "survival");

        assertEquals(box(10, 64, 10, 14, 68, 11), store.portals().getFirst().shape(), "min and max are picked per axis");
    }

    @Test
    @DisplayName("Mixed corner ordering per axis is sorted per axis")
    void cornersAreSortedPerAxis() {
        editor.corner1(ALICE, "p", new Pos(10, 68, 10));
        editor.corner2(ALICE, "p", new Pos(14, 64, 9));
        editor.task(ALICE, "p", "T");

        editor.save(ALICE, "p");

        assertEquals(box(10, 64, 9, 14, 68, 10), store.portals().getFirst().shape(), "each axis is ordered independently");
    }

    @Test
    @DisplayName("Corners use the block position, rounded down")
    void cornersFloorThePosition() {
        editor.corner1(ALICE, "p", new Pos(-0.5, 64.9, 3.99));
        editor.corner2(ALICE, "p", new Pos(-0.5, 64.9, 3.99));
        editor.task(ALICE, "p", "T");

        editor.save(ALICE, "p");

        assertEquals(box(-1, 64, 3, -1, 64, 3), store.portals().getFirst().shape(), "a single block box from the floored position");
    }

    @Test
    @DisplayName("A new corner replaces the earlier one")
    void newCornerReplacesOld() {
        editor.corner1(ALICE, "p", new Pos(0, 0, 0));
        editor.corner1(ALICE, "p", new Pos(5, 5, 5));
        editor.corner2(ALICE, "p", new Pos(6, 6, 6));
        editor.task(ALICE, "p", "T");

        editor.save(ALICE, "p");

        assertEquals(box(5, 5, 5, 6, 6, 6), store.portals().getFirst().shape(), "the last corner 1 counts");
    }

    @Test
    @DisplayName("A corner turns a ring draft into a box draft but keeps the saved ring until complete")
    void ringBecomesBox() {
        Portal ring = new Portal("p", new Disc(new Vec(0, 70, 0), 3, new Vec(0, 0, 1)), "T", null);
        store = new InMemoryPortalStore(ring);
        editor = new PortalEditor(store);

        PortalEditResult result = editor.corner1(ALICE, "p", new Pos(1, 2, 3));

        assertEquals(new Pending("p", List.of(Missing.CORNER_2)), result, "the box still needs its second corner");
        assertEquals(List.of(ring), store.portals(), "the saved ring stays until the box is saved");
        editor.corner2(ALICE, "p", new Pos(4, 5, 6));
        editor.save(ALICE, "p");
        assertEquals(box(1, 2, 3, 4, 5, 6), store.portals().getFirst().shape(), "the portal is now a box");
    }

    // --- ring -----------------------------------------------------------------------------

    @Test
    @DisplayName("disc uses the rounded eye position and the snapped normal")
    void discUsesEyeAndLook() {
        editor.disc(ALICE, "elytra-ring", new Pos(0.52, 72.0, 40.47), LOOK_Z, 5.5);
        editor.task(ALICE, "elytra-ring", "Elytra");

        editor.save(ALICE, "elytra-ring");

        assertEquals(new Disc(new Vec(0.5, 72.0, 40.5), 5.5, new Vec(0, 0, 1)), store.portals().getFirst().shape(), "centre, radius and normal follow the placement rules");
    }

    @Test
    @DisplayName("disc replaces a box and drops its corners")
    void discReplacesBox() {
        editor.corner1(ALICE, "p", new Pos(0, 0, 0));

        PortalEditResult result = editor.disc(ALICE, "p", new Pos(0, 70, 0), LOOK_Z, 2);

        assertEquals(new Pending("p", List.of(Missing.TASK)), result, "only the task is missing now");
        assertNull(editor.drafts(ALICE).getFirst().corner1(), "the corner is dropped");
    }

    @Test
    @DisplayName("centre without a radius leaves the radius missing")
    void centreWithoutRadiusIsPending() {
        PortalEditResult result = editor.centre(ALICE, "ring", new Pos(0, 70, 0), LOOK_Z);

        assertEquals(new Pending("ring", List.of(Missing.RADIUS, Missing.TASK)), result, "radius and task are missing");
    }

    @Test
    @DisplayName("radius changes only the radius, not centre or normal")
    void radiusChangesOnlyRadius() {
        editor.centre(ALICE, "ring", new Pos(0.5, 72, 40.5), LOOK_Z);
        editor.radius(ALICE, "ring", 3);

        editor.radius(ALICE, "ring", 8);

        PortalDraft draft = editor.drafts(ALICE).getFirst();
        assertEquals(8.0, draft.radius(), "the new radius");
        assertEquals(new Vec(0.5, 72, 40.5), draft.centre(), "centre is untouched");
        assertEquals(new Vec(0, 0, 1), draft.normal(), "normal is untouched");
    }

    @Test
    @DisplayName("radius on a ring without a centre asks for the centre")
    void radiusFirstAsksForCentre() {
        PortalEditResult result = editor.radius(ALICE, "ring", 4);

        assertEquals(new Pending("ring", List.of(Missing.CENTRE, Missing.TASK)), result, "the centre is still missing");
    }

    @Test
    @DisplayName("radius on a box draft is refused")
    void radiusOnBoxIsInvalid() {
        editor.corner1(ALICE, "p", new Pos(0, 0, 0));

        assertInstanceOf(Invalid.class, editor.radius(ALICE, "p", 3), "a box has no radius");
    }

    @Test
    @DisplayName("Radius zero, negative or not finite is refused and changes nothing")
    void invalidRadiusIsRefused() {
        editor.centre(ALICE, "ring", new Pos(0, 70, 0), LOOK_Z);
        editor.radius(ALICE, "ring", 3);

        for (double radius : new double[]{0, -2, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertInstanceOf(Invalid.class, editor.radius(ALICE, "ring", radius), "radius " + radius + " via radius");
            assertInstanceOf(Invalid.class, editor.disc(ALICE, "ring", new Pos(9, 9, 9), LOOK_Z, radius), "radius " + radius + " via disc");
        }
        assertEquals(3.0, editor.drafts(ALICE).getFirst().radius(), "the draft keeps the old radius");
        assertEquals(new Vec(0, 70, 0), editor.drafts(ALICE).getFirst().centre(), "a refused disc does not move the centre");
    }

    @Test
    @DisplayName("shape switches the form and drops what belongs to the other form")
    void shapeSwitchesForm() {
        editor.corner1(ALICE, "p", new Pos(0, 0, 0));

        PortalEditResult result = editor.shape(ALICE, "p", PortalDraft.Form.RING);

        assertEquals(new Pending("p", List.of(Missing.CENTRE, Missing.RADIUS, Missing.TASK)), result, "a fresh ring draft");
        assertNull(editor.drafts(ALICE).getFirst().corner1(), "the corner is dropped");
    }

    @Test
    @DisplayName("shape of the current form keeps the draft")
    void sameShapeKeepsDraft() {
        editor.corner1(ALICE, "p", new Pos(0, 0, 0));

        editor.shape(ALICE, "p", PortalDraft.Form.BOX);

        assertEquals(new Vec(0, 0, 0), editor.drafts(ALICE).getFirst().corner1(), "the corner survives choosing box again");
    }

    @Test
    @DisplayName("A draft without form and task lists both as missing")
    void permissionOnUnknownIdCreatesIncompleteDraft() {
        PortalEditResult result = editor.permission(ALICE, "new-one", "titan.x");

        assertEquals(new Pending("new-one", List.of(Missing.FORM, Missing.TASK)), result, "form and task are missing");
        assertEquals(1, editor.drafts(ALICE).size(), "a draft was created");
        assertInstanceOf(Pending.class, editor.save(ALICE, "new-one"), "it cannot be saved");
    }

    // --- task and permission --------------------------------------------------------------

    @Test
    @DisplayName("Task is trimmed and stored")
    void taskIsTrimmed() {
        completeBox(ALICE, "p");
        editor.task(ALICE, "p", "  Creative Lobby  ");

        editor.save(ALICE, "p");

        assertEquals("Creative Lobby", store.portals().getFirst().task(), "surrounding blanks are removed");
    }

    @Test
    @DisplayName("A blank task is refused and the old task stays")
    void blankTaskIsRefused() {
        completeBox(ALICE, "p");

        for (String task : List.of("", "   ", "\t")) {
            assertInstanceOf(Invalid.class, editor.task(ALICE, "p", task), "blank task '" + task + "' is refused");
        }
        assertEquals("Survival", editor.drafts(ALICE).getFirst().task(), "the previous task is kept");
    }

    @Test
    @DisplayName("Permission none removes the permission")
    void permissionNoneRemoves() {
        Portal saved = new Portal("survival", box(0, 0, 0, 1, 1, 1), "Survival", "titan.portal.survival");
        store = new InMemoryPortalStore(saved);
        editor = new PortalEditor(store);

        editor.permission(ALICE, "survival", "none");
        editor.save(ALICE, "survival");

        assertNull(store.portals().getFirst().permission(), "the portal is open for everyone");
    }

    @Test
    @DisplayName("A blank permission is refused")
    void blankPermissionIsRefused() {
        assertInstanceOf(Invalid.class, editor.permission(ALICE, "p", " "), "blank permission is refused");
        assertTrue(editor.drafts(ALICE).isEmpty(), "a refused input creates no draft");
    }

    // --- remove ---------------------------------------------------------------------------

    @Test
    @DisplayName("Remove deletes the saved portal and the player's draft")
    void removeDeletesSavedPortal() {
        Portal keep = new Portal("keep", box(0, 0, 0, 1, 1, 1), "K", null);
        Portal drop = new Portal("drop", box(2, 2, 2, 3, 3, 3), "D", null);
        store = new InMemoryPortalStore(keep, drop);
        editor = new PortalEditor(store);
        editor.task(ALICE, "drop", "Changed");

        PortalEditResult result = editor.remove(ALICE, "drop");

        assertEquals(new Removed("drop"), result, "removal is acknowledged");
        assertEquals(List.of(keep), store.portals(), "only the other portal remains");
        assertTrue(editor.drafts(ALICE).isEmpty(), "the draft is discarded too");
    }

    @Test
    @DisplayName("Remove of a draft-only id discards the draft without writing the map")
    void removeDraftOnly() {
        editor.corner1(ALICE, "p", new Pos(0, 0, 0));

        PortalEditResult result = editor.remove(ALICE, "p");

        assertEquals(new Removed("p"), result, "the draft counts as removed");
        assertEquals(0, store.saves(), "the map was not written");
    }

    @Test
    @DisplayName("Remove of an unknown id changes nothing")
    void removeUnknown() {
        assertEquals(new Unknown("nope"), editor.remove(ALICE, "nope"), "unknown id is reported");
        assertEquals(0, store.saves(), "the map was not written");
    }

    @Test
    @DisplayName("Remove keeps another player's draft")
    void removeKeepsOtherPlayersDraft() {
        editor.corner1(BOB, "p", new Pos(0, 0, 0));

        editor.remove(ALICE, "p");

        assertFalse(editor.drafts(BOB).isEmpty(), "Bob's draft is not Alice's to remove");
    }
}
