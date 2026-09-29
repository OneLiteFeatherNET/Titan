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
package net.onelitefeather.titan.feature.season;

import java.util.Optional;

/**
 * Decides what a mismatch between the started and the desired world means for the running lobby.
 * Pure: the lobby never switches worlds live, it only restarts while empty.
 */
final class RestartPolicy {

    enum Decision {
        /** Both worlds match: nothing is pending. */
        NONE,
        /** They differ, but players are online, so the lobby waits. */
        PENDING,
        /**
         * They differ and nobody is online: stop so the supervisor restarts into the desired world.
         */
        STOP
    }

    /**
     * @param startedWorld the world the lobby started with; empty for the default world
     * @param desiredWorld the world it should run in now; empty for the default world
     */
    Decision decide(Optional<String> startedWorld, Optional<String> desiredWorld, int onlinePlayers) {
        if (startedWorld.equals(desiredWorld)) {
            return Decision.NONE;
        }
        return onlinePlayers > 0 ? Decision.PENDING : Decision.STOP;
    }
}
