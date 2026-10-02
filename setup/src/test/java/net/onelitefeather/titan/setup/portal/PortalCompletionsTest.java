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

import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalCompletionsTest {

    private static Portal portal(String id, String task) {
        return new Portal(id, new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), task, null);
    }

    @Test
    @DisplayName("Ids come from the saved portals and the own drafts, without duplicates")
    void idsWithoutDuplicates() {
        List<String> ids = PortalCompletions.ids(List.of(portal("a", "x"), portal("b", "y")), List.of(new PortalDraft("b"), new PortalDraft("c")));

        assertEquals(List.of("a", "b", "c"), ids);
    }

    @Test
    @DisplayName("Without portals and drafts there are no ids")
    void noIds() {
        assertEquals(List.of(), PortalCompletions.ids(List.of(), List.of()));
    }

    @Test
    @DisplayName("Tasks are the distinct tasks of the saved portals in list order")
    void distinctTasks() {
        assertEquals(List.of("Survival", "Elytra"), PortalCompletions.tasks(List.of(portal("a", "Survival"), portal("b", "Elytra"), portal("c", "Survival"))));
    }

    @Test
    @DisplayName("Radius hints are 1, 2, 3, 5 and 8")
    void radii() {
        assertEquals(List.of("1", "2", "3", "5", "8"), PortalCompletions.radii());
    }

    @Test
    @DisplayName("The permission hint is none")
    void permissions() {
        assertEquals(List.of("none"), PortalCompletions.permissions());
    }

    @Test
    @DisplayName("Label source types are task, group, service and local")
    void sourceTypes() {
        assertEquals(List.of("task", "group", "service", "local"), PortalCompletions.sourceTypes());
    }
}
