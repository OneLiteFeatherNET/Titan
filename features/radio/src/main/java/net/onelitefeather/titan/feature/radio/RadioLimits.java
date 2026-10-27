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
package net.onelitefeather.titan.feature.radio;

/**
 * The bounds of the radio. Songs have no size, count or length limit; the only bound left is the
 * number of notes a player receives per tick, which keeps the packets per player within what a
 * lobby can carry.
 */
final class RadioLimits {

    static final int TICKS_PER_SECOND = 20;
    static final int MAX_NOTES_PER_TICK = 16;
    /**
     * A vanilla disc's client may run a little past the table length; the server cuts it after
     * this.
     */
    static final int DISC_END_BUFFER_TICKS = 5 * TICKS_PER_SECOND;

    private RadioLimits() {
    }
}
