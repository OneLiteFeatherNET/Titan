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
package net.onelitefeather.titan.feature.jumprun;

import java.util.Map;
import java.util.UUID;

/**
 * Where finished runs are kept. Every method may block, so none may be called on the tick thread.
 * A failure surfaces as an unchecked exception; the caller decides what to log.
 */
interface RunStore {

    /**
     * The best score of the player in each mode they have played; a mode never played is absent.
     */
    Map<Mode, Integer> bestsOf(UUID player);

    /**
     * The best three players of every mode with at least one run, each player once with their best
     * run (the earlier of equal scores) under the name of their latest run; a mode without runs is
     * absent.
     */
    Map<Mode, TopThree> topThreeOfEveryMode();

    /** Keeps a finished run, whatever its score. */
    void append(FinishedRun run);
}
