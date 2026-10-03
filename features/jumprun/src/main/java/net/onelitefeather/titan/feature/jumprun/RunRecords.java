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

import java.util.OptionalInt;
import java.util.UUID;

/** Best score per player and mode; the seam to where records are kept. */
interface RunRecords {

    OptionalInt best(UUID player, Mode mode);

    /**
     * Counts the run if its score beats the best of its mode so far; returns {@code true} on a new
     * record. Whether the run is kept beyond the session is up to the implementation.
     */
    boolean submit(FinishedRun run);

    /**
     * Gets the player's bests ready before their first run; may block, so never on the tick thread.
     */
    void load(UUID player);

    /** Drops the player's bests of every mode; the record lasts only for the session. */
    void forget(UUID player);
}
