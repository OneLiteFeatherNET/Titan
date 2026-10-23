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
package net.onelitefeather.titan.feature.npc;

import java.util.List;
import java.util.Optional;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JoinChoiceTest {

    @DisplayName("A click without the permission is denied before any service is looked at")
    @Test
    void deniedWithoutPermission() {
        JoinChoice choice = JoinChoice.of(false, Optional.of(List.of(new ServiceCount("Slender-1", 3, 20))));
        Assertions.assertEquals(JoinChoice.Result.DENIED, choice.result(), "a player without the permission is never sent");
        Assertions.assertNull(choice.service(), "no service is chosen for a denied click");
    }

    @DisplayName("An unreadable task is unavailable, not none ready")
    @Test
    void unreadableIsUnavailable() {
        Assertions.assertEquals(JoinChoice.Result.UNAVAILABLE, JoinChoice.of(true, Optional.empty()).result(), "a failed read must not look like a task that runs nowhere");
    }

    @DisplayName("A task whose only service is full is none ready")
    @Test
    void onlyFullServiceIsNoneReady() {
        Assertions.assertEquals(JoinChoice.Result.NONE_READY, JoinChoice.of(true, Optional.of(List.of(new ServiceCount("Slender-1", 20, 20)))).result(), "a full service cannot be joined");
    }

    @DisplayName("A click joins the ready service with the most players, as the quick join does")
    @Test
    void joinsMostPopulatedReadyService() {
        JoinChoice choice = JoinChoice.of(true, Optional.of(List.of(new ServiceCount("Slender-1", 4, 20), new ServiceCount("Slender-2", 12, 20), new ServiceCount("Slender-3", 30, 0))));
        Assertions.assertEquals(JoinChoice.Result.JOINED, choice.result(), "a ready service exists");
        Assertions.assertEquals("Slender-2", choice.service().name(), "the most populated ready service is joined, a service without a limit is not ready");
    }
}
