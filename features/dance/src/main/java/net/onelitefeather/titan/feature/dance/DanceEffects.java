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
package net.onelitefeather.titan.feature.dance;

import net.minestom.server.coordinate.Pos;

/**
 * What a dance does to its dancer. {@link MinestomDanceEffects} is the real one; the seam exists so
 * a test can make a step fail and check that the stop path still cleans up.
 */
interface DanceEffects {

    /**
     * Performs {@code action} for the dancer.
     *
     * @param anchor the dancer's position and view when the dance started
     */
    void apply(DanceAction action, Pos anchor);

    /**
     * Brings the dancer back to standing and to the view he had when the dance started.
     *
     * @param anchor the dancer's position and view when the dance started
     */
    void reset(Pos anchor);
}
