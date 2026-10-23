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

import java.util.Arrays;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The dances of the lobby, fixed in code. The id is what the client sends back in a dance button;
 * the permission, when present, gates both the button and the start.
 */
enum Dance {

    WAVE("wave", null, "titan.dance.label.wave", DanceSequence.lasting(60).every(20, 0, 60, DanceAction.SWING_MAIN_HAND).alternating(10, 0, 60, DanceAction.HEAD_DOWN, DanceAction.HEAD_UP).build()),

    GROOVE("groove", null, "titan.dance.label.groove", DanceSequence.lasting(80).alternating(10, 0, 80, DanceAction.SNEAK, DanceAction.STAND).alternating(10, 0, 80, DanceAction.SWING_MAIN_HAND, DanceAction.SWING_OFF_HAND).every(5, 0, 80, DanceAction.NOTE).build()),

    PARTY("party", null, "titan.dance.label.party", DanceSequence.lasting(80).every(20, 0, 80, DanceAction.JUMP).every(10, 0, 80, DanceAction.NOTE).every(20, 10, 80, DanceAction.HEART).build()),

    // The NPC hangs in the air for the first 40 ticks, then lands and the spin ends.
    SPIN("spin", "titan.dance.spin", "titan.dance.label.spin", DanceSequence.lasting(60).at(0, DanceAction.SPIN).every(10, 0, 60, DanceAction.HEART).at(40, DanceAction.SPIN_LAND).build());

    private final String id;
    private final @Nullable String permission;
    private final String labelKey;
    private final DanceSequence sequence;

    Dance(String id, @Nullable String permission, String labelKey, DanceSequence sequence) {
        this.id = id;
        this.permission = permission;
        this.labelKey = labelKey;
        this.sequence = sequence;
    }

    String id() {
        return this.id;
    }

    /** @return the permission node a player needs, or empty for a dance everybody may use */
    Optional<String> permission() {
        return Optional.ofNullable(this.permission);
    }

    /** @return the translation key of the dance's button label */
    String labelKey() {
        return this.labelKey;
    }

    DanceSequence sequence() {
        return this.sequence;
    }

    static Optional<Dance> byId(String id) {
        return Arrays.stream(values()).filter(dance -> dance.id.equals(id)).findFirst();
    }
}
