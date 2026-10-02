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
package net.onelitefeather.titan.core.portal;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelSourceTest {

    private static final Box BOX = new Box(new Vec(10, 64, 10), new Vec(14, 68, 11));

    private static Portal portal(PortalLabel label) {
        return new Portal("survival", BOX, "Survival", null, label);
    }

    private static PortalLabel label(LabelSource source) {
        return new PortalLabel(new Vec(12.5, 66, -3.5), "<gray>Survival", null, source, Billboard.CENTER, 0);
    }

    @DisplayName("A label without a source counts the players of the portal's task")
    @Test
    void missingSourceFallsBackToPortalTask() {
        assertEquals(new LabelSource.Task("Survival"), LabelSource.orDefault(portal(label(null))), "a missing source must mean the portal's task");
    }

    @DisplayName("A label's own source wins over the portal's task")
    @Test
    void explicitSourceIsKept() {
        LabelSource group = new LabelSource.Group("Lobby");

        assertEquals(group, LabelSource.orDefault(portal(label(group))), "an explicit source must not be replaced");
    }

    @DisplayName("A portal without a label still resolves to its task")
    @Test
    void portalWithoutLabelFallsBackToTask() {
        assertEquals(new LabelSource.Task("Survival"), LabelSource.orDefault(portal(null)), "no label must not break the lookup");
    }

    @DisplayName("Every type of the vocabulary builds its own source; local ignores the name")
    @Test
    void ofBuildsEveryKnownType() {
        assertEquals(new LabelSource.Task("T"), LabelSource.of("task", "T"), "task");
        assertEquals(new LabelSource.Group("G"), LabelSource.of("group", "G"), "group");
        assertEquals(new LabelSource.Service("S"), LabelSource.of("service", "S"), "service");
        assertEquals(new LabelSource.Local(), LabelSource.of("local", "ignored"), "local");
        assertEquals(4, LabelSource.TYPES.size(), "the vocabulary is fixed");
    }

    @DisplayName("An unknown or missing type becomes Unknown so the validator can name it")
    @Test
    void ofKeepsUnknownTypes() {
        assertEquals(new LabelSource.Unknown("proxy"), LabelSource.of("proxy", "x"), "unknown type");
        assertEquals(new LabelSource.Unknown(null), LabelSource.of(null, null), "missing type");
    }
}
