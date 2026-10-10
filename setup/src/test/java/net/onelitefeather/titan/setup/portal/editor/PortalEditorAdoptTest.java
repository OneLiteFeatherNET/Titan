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
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.InMemoryPortalStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalEditorAdoptTest {

    private static final UUID ALICE = new UUID(0, 1);
    private static final Portal SURVIVAL = new Portal("survival", new Box(new Vec(10, 64, 10), new Vec(14, 68, 11)), "Survival", "titan.portal.survival");

    private InMemoryPortalStore store;
    private PortalEditor editor;

    @BeforeEach
    void freshFixture() {
        store = new InMemoryPortalStore();
        editor = new PortalEditor(store);
    }

    @Test
    @DisplayName("Adopting a portal opens a draft with its values and no guided flow")
    void adoptOpensDraftWithThePortalsValues() {
        Optional<String> refusal = editor.adopt(ALICE, SURVIVAL);

        assertTrue(refusal.isEmpty(), "a free id is adopted");
        PortalDraft draft = editor.draft(ALICE, "survival").orElseThrow();
        assertEquals(Optional.of(SURVIVAL), draft.toPortal(), "the draft holds the portal's values unchanged");
        assertFalse(draft.guided(), "a copied draft does not start the guided flow");
    }

    @Test
    @DisplayName("Adopting does not write to the store")
    void adoptDoesNotWriteTheStore() {
        editor.adopt(ALICE, SURVIVAL);

        assertEquals(0, store.saves(), "the map is only written by save");
        assertTrue(store.portals().isEmpty(), "the stored portals stay as they were");
    }

    @Test
    @DisplayName("Adopting an id with an open draft keeps that draft and reports the id as skipped")
    void adoptKeepsAnOpenDraft() {
        editor.task(ALICE, "survival", "Mine");

        Optional<String> refusal = editor.adopt(ALICE, SURVIVAL);

        assertTrue(refusal.isPresent(), "an id with an open draft is not adopted");
        assertEquals("Mine", editor.draft(ALICE, "survival").orElseThrow().task(), "the player's own draft is unchanged");
    }

    @Test
    @DisplayName("Adopting a reserved word as id is refused and creates no draft")
    void adoptRefusesReservedIds() {
        for (String reserved : new String[]{"copy", "save-all", "list", "show", "create"}) {
            Portal portal = new Portal(reserved, SURVIVAL.shape(), "Survival", null);

            assertTrue(editor.adopt(ALICE, portal).isPresent(), "'" + reserved + "' must be refused");
        }
        assertTrue(editor.drafts(ALICE).isEmpty(), "no draft is created for a refused id");
    }

    @Test
    @DisplayName("Create refuses copy and save-all as ids")
    void createRefusesCopyAndSaveAll() {
        assertTrue(editor.create(ALICE, "copy") instanceof PortalEditResult.Invalid, "'copy' is reserved");
        assertTrue(editor.create(ALICE, "save-all") instanceof PortalEditResult.Invalid, "'save-all' is reserved");
        assertTrue(editor.drafts(ALICE).isEmpty(), "no draft is created for a reserved id");
    }

    @Test
    @DisplayName("Adopting an id the editor cannot take refuses it with the editor's reason")
    void adoptRefusesIdsOutsideThePattern() {
        Portal upper = new Portal("Survival", SURVIVAL.shape(), "Survival", null);

        assertTrue(editor.adopt(ALICE, upper).isPresent(), "an id with capitals is refused");
        assertTrue(editor.drafts(ALICE).isEmpty(), "no draft is created for it");
    }

    @Test
    @DisplayName("Adopting a portal of a ring keeps centre, normal and radius")
    void adoptKeepsRing() {
        Portal ring = new Portal("ring", new Disc(new Vec(0.5, 72, 40.5), 5.5, new Vec(0, 0, 1)), "Elytra", null);

        editor.adopt(ALICE, ring);

        assertEquals(Optional.of(ring), editor.draft(ALICE, "ring").orElseThrow().toPortal(), "the ring round-trips through the draft");
    }

    @Test
    @DisplayName("A complete draft from an adopted portal is complete but not saved")
    void adoptedDraftIsCompleteButNotSaved() {
        editor.adopt(ALICE, SURVIVAL);

        assertTrue(editor.draft(ALICE, "survival").orElseThrow().complete(), "all parts came with the portal");
        assertEquals(0, store.saves(), "complete is not saved");
    }
}
