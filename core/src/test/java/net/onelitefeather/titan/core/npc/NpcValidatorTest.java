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
package net.onelitefeather.titan.core.npc;

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NpcValidatorTest {

    private static LobbyNpc npc(String id, String task, Pos position, NpcSkin skin, PortalLabel label, String permission) {
        return new LobbyNpc(id, task, position, skin, label, false, permission);
    }

    private static LobbyNpc valid() {
        return npc("slender", "Slender", new Pos(76.5, 80, 157.5, -90, 0), new NpcSkin("dGV4dHVyZQ==", "sig"), null, "titan.navigator.slender");
    }

    @DisplayName("A complete NPC has no problems")
    @Test
    void validNpcHasNoProblems() {
        Assertions.assertEquals(List.of(), NpcValidator.problems(valid()), "a complete NPC is usable");
    }

    @DisplayName("An NPC without skin, label and permission is usable")
    @Test
    void optionalPartsMayBeMissing() {
        Assertions.assertEquals(List.of(), NpcValidator.problems(npc("guide", "Lobby", new Pos(0, 66, 0), null, null, null)), "skin, label and permission are optional");
    }

    @DisplayName("A blank task is a problem named by its field")
    @Test
    void blankTaskIsReported() {
        Assertions.assertEquals(List.of("task must not be blank"), NpcValidator.problems(npc("slender", " ", new Pos(0, 80, 0), null, null, null)), "the task is required");
    }

    @DisplayName("A missing position is a problem")
    @Test
    void missingPositionIsReported() {
        Assertions.assertEquals(List.of("position is missing"), NpcValidator.problems(npc("slender", "Slender", null, null, null, null)), "the NPC needs a place to stand");
    }

    @DisplayName("A skin without a value is a problem")
    @Test
    void blankSkinValueIsReported() {
        Assertions.assertEquals(List.of("skin.value must not be blank"), NpcValidator.problems(npc("slender", "Slender", new Pos(0, 80, 0), new NpcSkin(" ", null), null, null)), "a skin needs its texture");
    }

    @DisplayName("A blank permission is a problem, a missing one is not")
    @Test
    void blankPermissionIsReported() {
        Assertions.assertEquals(List.of("permission must not be blank when given"), NpcValidator.problems(npc("slender", "Slender", new Pos(0, 80, 0), null, null, "")), "an empty permission would lock everyone out silently");
    }

    @DisplayName("A label with an unknown tag is reported with its field")
    @Test
    void labelProblemsAreReported() {
        PortalLabel label = new PortalLabel(new Vec(0, 81, 0), "<nosuchtag>Slender", null, new LabelSource.Task("Slender"), Billboard.CENTER, 0f);
        List<String> problems = NpcValidator.problems(npc("slender", "Slender", new Pos(0, 80, 0), null, label, null));
        Assertions.assertTrue(problems.stream().anyMatch(problem -> problem.startsWith("label.text")), "the label text must be checked: " + problems);
    }
}
