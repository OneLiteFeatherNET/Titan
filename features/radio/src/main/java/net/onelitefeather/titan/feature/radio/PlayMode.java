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
 * What happens when a title ends: nothing, the same title again, or a random other title. The
 * mode belongs to the player's session and is forgotten at logout.
 */
enum PlayMode {
    ONCE("once"), LOOP("loop"), SHUFFLE("shuffle");

    /** A new session plays once: the radio plays only what the player chose. */
    static final PlayMode DEFAULT = ONCE;

    private final String telemetryName;

    PlayMode(String telemetryName) {
        this.telemetryName = telemetryName;
    }

    /** The next mode in the order the mode button cycles through. */
    PlayMode next() {
        PlayMode[] modes = values();
        return modes[(this.ordinal() + 1) % modes.length];
    }

    String telemetryName() {
        return this.telemetryName;
    }
}
