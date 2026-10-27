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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayModeTest {

    @DisplayName("A new session plays once")
    @Test
    void defaultIsOnce() {
        Assertions.assertEquals(PlayMode.ONCE, PlayMode.DEFAULT, "the default play mode must be once");
    }

    @DisplayName("The mode button cycles once, loop and shuffle, then starts over")
    @Test
    void nextCyclesThroughTheModes() {
        Assertions.assertEquals(PlayMode.LOOP, PlayMode.ONCE.next(), "once must be followed by loop");
        Assertions.assertEquals(PlayMode.SHUFFLE, PlayMode.LOOP.next(), "loop must be followed by shuffle");
        Assertions.assertEquals(PlayMode.ONCE, PlayMode.SHUFFLE.next(), "shuffle must be followed by once");
    }

    @DisplayName("Each mode has the name telemetry counts it by")
    @Test
    void telemetryNamesAreTheModeNames() {
        Assertions.assertEquals("once", PlayMode.ONCE.telemetryName(), "once must count as once");
        Assertions.assertEquals("loop", PlayMode.LOOP.telemetryName(), "loop must count as loop");
        Assertions.assertEquals("shuffle", PlayMode.SHUFFLE.telemetryName(), "shuffle must count as shuffle");
    }
}
