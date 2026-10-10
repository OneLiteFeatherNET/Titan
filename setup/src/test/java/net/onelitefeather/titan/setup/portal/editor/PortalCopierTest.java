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

import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.InMemoryPortalSources;
import net.onelitefeather.titan.setup.portal.InMemoryPortalStore;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.Copied;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.NothingToCopy;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.SameWorld;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.UnknownWorld;
import net.onelitefeather.titan.setup.portal.editor.CopyResult.Unreadable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalCopierTest {

    private static final UUID ALICE = new UUID(0, 1);
    private static final Portal SURVIVAL = portal("survival", "Survival");
    private static final Portal CREATIVE = portal("creative", "Creative");

    private InMemoryPortalStore store;
    private PortalEditor editor;
    private InMemoryPortalSources sources;
    private PortalCopier copier;

    @BeforeEach
    void freshFixture() {
        store = new InMemoryPortalStore();
        editor = new PortalEditor(store);
        sources = new InMemoryPortalSources("winter").world("lobby", SURVIVAL, CREATIVE);
        copier = new PortalCopier(editor, sources, store);
    }

    private static Portal portal(String id, String task) {
        return new Portal(id, new Box(new Vec(10, 64, 10), new Vec(14, 68, 11)), task, null);
    }

    @Test
    @DisplayName("Copying opens one draft per source portal with the source's values")
    void copyOpensOneDraftPerPortal() {
        CopyResult result = copier.copy(ALICE, "lobby");

        Copied copied = assertInstanceOf(Copied.class, result, "a readable other world is copied");
        assertEquals(List.of(SURVIVAL, CREATIVE), copied.portals(), "the adopted portals are reported in source order");
        assertEquals(List.of("survival", "creative"), copied.added(), "both ids are new in the target world");
        assertEquals(SURVIVAL, editor.draft(ALICE, "survival").orElseThrow().toPortal().orElseThrow(), "the draft holds the source's values");
        assertEquals(CREATIVE, editor.draft(ALICE, "creative").orElseThrow().toPortal().orElseThrow(), "the draft holds the source's values");
    }

    @Test
    @DisplayName("Copying writes nothing to the target world's store")
    void copyLeavesTheStoreUntouched() {
        copier.copy(ALICE, "lobby");

        assertEquals(0, store.saves(), "the store is only written by save");
        assertTrue(store.portals().isEmpty(), "no portal is in the target world yet");
    }

    @Test
    @DisplayName("A copied id that the target world already has is reported as replacing on save")
    void copyReportsReplacedIds() {
        store = new InMemoryPortalStore(portal("survival", "Old"));
        editor = new PortalEditor(store);
        copier = new PortalCopier(editor, sources, store);

        Copied copied = assertInstanceOf(Copied.class, copier.copy(ALICE, "lobby"));

        assertEquals(List.of("survival"), copied.replacing(), "survival exists in the target world");
        assertEquals(List.of("creative"), copied.added(), "creative is new");
        assertEquals(List.of(new Portal("survival", new Box(new Vec(10, 64, 10), new Vec(14, 68, 11)), "Old", null)), store.portals(), "the saved portal stays until save");
    }

    @Test
    @DisplayName("An id with an open draft is skipped and the player's draft stays as it was")
    void copySkipsAnOpenDraft() {
        editor.task(ALICE, "survival", "Mine");

        Copied copied = assertInstanceOf(Copied.class, copier.copy(ALICE, "lobby"));

        assertEquals(List.of(CREATIVE), copied.portals(), "only the portal without a draft is copied");
        assertEquals(1, copied.skipped().size(), "the id with the open draft is reported");
        assertEquals("survival", copied.skipped().getFirst().id(), "the skipped id is named");
        assertEquals("Mine", editor.draft(ALICE, "survival").orElseThrow().task(), "the open draft is not overwritten");
    }

    @Test
    @DisplayName("A source id the command reserves is skipped with its reason and the others are copied")
    void copySkipsReservedIds() {
        sources = new InMemoryPortalSources("winter").world("lobby", portal("copy", "Bad"), CREATIVE);
        copier = new PortalCopier(editor, sources, store);

        Copied copied = assertInstanceOf(Copied.class, copier.copy(ALICE, "lobby"));

        assertEquals(List.of(CREATIVE), copied.portals(), "the reserved id is not copied");
        assertEquals("copy", copied.skipped().getFirst().id(), "the reserved id is named as skipped");
        assertTrue(copied.skipped().getFirst().reason().contains("reserved"), "the reason says why: " + copied.skipped().getFirst().reason());
        assertTrue(editor.draft(ALICE, "copy").isEmpty(), "no draft is created for the reserved id");
    }

    @Test
    @DisplayName("An unknown world is reported with the worlds that are available")
    void unknownWorldListsTheAvailable() {
        sources = new InMemoryPortalSources("winter").world("lobby", SURVIVAL).world("summer", CREATIVE);
        copier = new PortalCopier(editor, sources, store);

        CopyResult result = copier.copy(ALICE, "nirgendwo");

        assertEquals(new UnknownWorld("nirgendwo", List.of("lobby", "summer")), result, "the unknown name and the available worlds are given");
        assertTrue(editor.drafts(ALICE).isEmpty(), "no drafts for an unknown world");
    }

    @Test
    @DisplayName("The loaded world is refused as source, also with other letter case")
    void loadedWorldIsRefused() {
        assertInstanceOf(SameWorld.class, copier.copy(ALICE, "winter"), "the loaded world is the target");
        assertInstanceOf(SameWorld.class, copier.copy(ALICE, "Winter"), "letter case does not make it another world");
        assertTrue(editor.drafts(ALICE).isEmpty(), "no drafts are created");
    }

    @Test
    @DisplayName("A source without portals is nothing to copy")
    void sourceWithoutPortalsIsNothingToCopy() {
        sources = new InMemoryPortalSources("winter").world("empty");
        copier = new PortalCopier(editor, sources, store);

        assertInstanceOf(NothingToCopy.class, copier.copy(ALICE, "empty"), "an empty world has nothing to copy");
        assertTrue(editor.drafts(ALICE).isEmpty(), "no drafts are created");
    }

    @Test
    @DisplayName("An unreadable source is reported with its reason and changes nothing")
    void unreadableSourceIsReported() {
        sources = new InMemoryPortalSources("winter").unreadable("broken");
        copier = new PortalCopier(editor, sources, store);

        CopyResult result = copier.copy(ALICE, "broken");

        Unreadable unreadable = assertInstanceOf(Unreadable.class, result, "a broken map file is reported");
        assertTrue(unreadable.reason().contains("broken"), "the reason names the world: " + unreadable.reason());
        assertTrue(editor.drafts(ALICE).isEmpty(), "no drafts are created");
    }
}
