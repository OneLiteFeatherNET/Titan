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

/** Best score per player; the seam for a later persistent stats service. */
interface RunRecords {

    OptionalInt best(UUID player);

    /** Stores the score if it beats the best so far; returns {@code true} on a new record. */
    boolean submit(UUID player, int score);

    /** Drops the player's best; the record lasts only for the session. */
    void forget(UUID player);
}
