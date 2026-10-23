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
package net.onelitefeather.titan.core.module;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LobbyAtmosphereTest {

    @DisplayName("The configured key slender-fog resolves to the slender fog")
    @Test
    void slenderFogKeyIsResolved() {
        Assertions.assertEquals(Optional.of(LobbyAtmosphere.SLENDER_FOG), LobbyAtmosphere.fromKey("slender-fog"), "slender-fog must resolve to SLENDER_FOG");
    }

    @DisplayName("An unknown key resolves to no atmosphere")
    @Test
    void unknownKeyResolvesToNothing() {
        Assertions.assertEquals(Optional.empty(), LobbyAtmosphere.fromKey("fog"), "an unknown key must not resolve");
    }

    @DisplayName("Keys are case sensitive, as the configuration values are written")
    @Test
    void keysAreCaseSensitive() {
        Assertions.assertEquals(Optional.empty(), LobbyAtmosphere.fromKey("Slender-Fog"), "a differently cased key must not resolve");
    }

    @DisplayName("The slender fog reports the key it is configured with")
    @Test
    void slenderFogReportsItsKey() {
        Assertions.assertEquals("slender-fog", LobbyAtmosphere.SLENDER_FOG.key(), "the configuration key of SLENDER_FOG must be slender-fog");
    }
}
