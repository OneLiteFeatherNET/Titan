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
package net.onelitefeather.titan.platform.otis;

import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtisSettingsTest {

    private static final String SECRET = "s3cret-otis-token";

    @DisplayName("The environment variable wins over the configured token")
    @Test
    void environmentTokenWins() {
        Assertions.assertEquals("from-env", OtisSettings.resolveToken("from-env", "from-config"), "TITAN_OTIS_TOKEN must take precedence");
    }

    @DisplayName("A blank environment variable falls back to the configured token")
    @Test
    void blankEnvironmentFallsBack() {
        Assertions.assertEquals("from-config", OtisSettings.resolveToken("  ", "from-config"), "an empty variable must not hide the config value");
    }

    @DisplayName("Without any token the lobby calls Otis without one")
    @Test
    void noTokenAnywhere() {
        Assertions.assertEquals("", OtisSettings.resolveToken(null, null), "no token must resolve to an empty string");
    }

    @DisplayName("The settings text shows that a token is set, never the token")
    @Test
    void toStringMasksTheToken() {
        OtisSettings settings = settings(SECRET);
        Assertions.assertFalse(settings.toString().contains(SECRET), "the token must not appear in toString");
        Assertions.assertTrue(settings.toString().contains("token=****"), "a set token must show as masked");
    }

    @DisplayName("Without a token the settings text says so")
    @Test
    void toStringReportsAnUnsetToken() {
        Assertions.assertTrue(settings("").toString().contains("token=unset"), "an unset token must be reported as unset");
    }

    @DisplayName("A zero request timeout aborts the start")
    @Test
    void zeroRequestTimeoutIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new OtisSettings("http://otis", "", Duration.ofSeconds(1), Duration.ZERO, 1, 3, Duration.ofSeconds(30)), "a request timeout must be positive");
    }

    private static OtisSettings settings(String token) {
        return new OtisSettings("http://otis:8080", token, Duration.ofSeconds(1), Duration.ofSeconds(2), 1, 3, Duration.ofSeconds(30));
    }
}
