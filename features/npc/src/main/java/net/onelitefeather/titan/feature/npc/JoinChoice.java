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
import net.onelitefeather.titan.core.portal.TaskJoin;
import org.jetbrains.annotations.Nullable;

/**
 * What a click on a join NPC does, decided from the permission and one reading of the task's
 * services. The service is the one the quick join of the navigator would pick.
 *
 * @param result  the outcome, which also names the message or the metric
 * @param service the service to join, present only for {@link Result#JOINED}
 */
record JoinChoice(Result result, @Nullable ServiceCount service) {

    /** The outcome of one click. */
    enum Result {
        /** The player goes to {@link #service()}. */
        JOINED,
        /** The task runs, but no service is ready and not full. */
        NONE_READY,
        /** The services could not be read at all. */
        UNAVAILABLE,
        /** The player lacks the permission the NPC asks for. */
        DENIED
    }

    /**
     * The choice for a click: a missing permission is denied before anything is read; an unreadable
     * task is unavailable; otherwise the joinable service the navigator would pick, or none ready.
     */
    static JoinChoice of(boolean permitted, Optional<List<ServiceCount>> services) {
        if (!permitted) {
            return new JoinChoice(Result.DENIED, null);
        }
        if (services.isEmpty()) {
            return new JoinChoice(Result.UNAVAILABLE, null);
        }
        Optional<ServiceCount> service = TaskJoin.pick(services.get());
        return service.map(picked -> new JoinChoice(Result.JOINED, picked)).orElseGet(() -> new JoinChoice(Result.NONE_READY, null));
    }
}
