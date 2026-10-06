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
import net.onelitefeather.titan.core.portal.PortalValidator;
import net.onelitefeather.titan.setup.portal.InMemoryPortalStore;
import net.onelitefeather.titan.setup.portal.editor.PortalEditResult.Rejected;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the editor and the lobby's start-up check in step: the editor has no rule copy, so what it
 * accepts must pass {@link PortalValidator#requireValid} and what it rejects must be the same
 * problem the validator reports.
 */
class PortalEditorValidatorParityTest {

    private static final UUID PLAYER = new UUID(0, 1);
    private static final Vec LOOK = new Vec(0.3, -0.2, 0.9);

    @Test
    @DisplayName("Every portal the editor accepts passes requireValid")
    void acceptedPortalsPassRequireValid() {
        InMemoryPortalStore store = new InMemoryPortalStore();
        PortalEditor editor = new PortalEditor(store);
        editor.corner1(PLAYER, "box-a", new Pos(14, 68, 11));
        editor.corner2(PLAYER, "box-a", new Pos(10, 64, 10));
        editor.task(PLAYER, "box-a", "Survival");
        editor.disc(PLAYER, "ring-a", new Pos(0.52, 72.0, 40.47), LOOK, 5.5);
        editor.task(PLAYER, "ring-a", "Elytra");
        editor.permission(PLAYER, "ring-a", "titan.portal.elytra");
        editor.centre(PLAYER, "ring-b", new Pos(-3.3, 65, 7.7), new Vec(0, 1, 0));
        editor.radius(PLAYER, "ring-b", 0.5);
        editor.task(PLAYER, "ring-b", "Creative");

        for (String id : List.of("box-a", "ring-a", "ring-b")) {
            assertTrue(editor.save(PLAYER, id) instanceof PortalEditResult.Saved, id + " is accepted");
        }

        assertEquals(3, store.portals().size(), "all three portals were written");
        assertDoesNotThrow(() -> PortalValidator.requireValid("test-world", store.portals()), "the lobby accepts what the editor saved");
    }

    @Test
    @DisplayName("A blank stored task is rejected by the editor and by requireValid with the same reason")
    void blankTaskIsRejectedByBoth() {
        List<Portal> invalid = List.of(new Portal("a", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), " ", null));
        InMemoryPortalStore store = new InMemoryPortalStore(invalid.toArray(Portal[]::new));
        PortalEditor editor = new PortalEditor(store);
        editor.permission(PLAYER, "a", "titan.x");

        PortalEditResult result = editor.save(PLAYER, "a");

        Rejected rejected = assertInstanceOf(Rejected.class, result, "the editor rejects the save");
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PortalValidator.requireValid("test-world", invalid), "the lobby rejects the list");
        String reason = rejected.problems().getFirst().reason();
        assertEquals("task must not be blank", reason, "the editor reports the validator's reason");
        assertTrue(failure.getMessage().contains(reason), "requireValid names the same reason: " + failure.getMessage());
        assertEquals(invalid, store.portals(), "the map is unchanged");
    }

    @Test
    @DisplayName("A stored zero normal is rejected by the editor and by requireValid with the same reason")
    void zeroNormalIsRejectedByBoth() {
        List<Portal> invalid = List.of(new Portal("ring", new Disc(new Vec(0, 70, 0), 3, Vec.ZERO), "Survival", null));
        InMemoryPortalStore store = new InMemoryPortalStore(invalid.toArray(Portal[]::new));
        PortalEditor editor = new PortalEditor(store);
        editor.permission(PLAYER, "ring", "titan.x");

        PortalEditResult result = editor.save(PLAYER, "ring");

        Rejected rejected = assertInstanceOf(Rejected.class, result, "the editor rejects the save");
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PortalValidator.requireValid("test-world", invalid), "the lobby rejects the list");
        assertTrue(failure.getMessage().contains(rejected.problems().getFirst().reason()), "requireValid names the editor's reason: " + failure.getMessage());
    }
}
