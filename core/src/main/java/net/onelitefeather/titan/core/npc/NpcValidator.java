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

import java.util.ArrayList;
import java.util.List;
import net.onelitefeather.titan.core.portal.PortalValidator;
import org.jetbrains.annotations.Nullable;

/**
 * The reasons a join NPC is unusable. The lobby skips such an entry with a warning naming these
 * reasons, so the other NPCs of the world still start.
 */
public final class NpcValidator {

    private NpcValidator() {
    }

    /** Every reason the NPC is unusable; empty if it is fine. */
    public static List<String> problems(LobbyNpc npc) {
        List<String> problems = new ArrayList<>();
        if (isBlank(npc.id())) {
            problems.add("id must not be blank");
        }
        if (isBlank(npc.task())) {
            problems.add("task must not be blank");
        }
        if (npc.position() == null) {
            problems.add("position is missing");
        }
        if (npc.skin() != null && isBlank(npc.skin().value())) {
            problems.add("skin.value must not be blank");
        }
        if (npc.permission() != null && isBlank(npc.permission())) {
            problems.add("permission must not be blank when given");
        }
        if (npc.label() != null) {
            problems.addAll(PortalValidator.labelProblems(npc.label()));
        }
        return problems;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
