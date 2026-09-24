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
package net.onelitefeather.titan.app.feature.example;

import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit coverage for {@link ExampleConfig}'s compact constructor - validation belongs to the
 * record itself, not to {@link net.onelitefeather.titan.common.config.ConfigStore}, so it is
 * testable without one; see {@code docs/lobby-modules.md}.
 */
class ExampleConfigTest {

    @DisplayName("DEFAULTS is itself a valid config")
    @Test
    void defaultsIsValid() {
        Assertions.assertDoesNotThrow(() -> new ExampleConfig(ExampleConfig.DEFAULTS.greeting(), ExampleConfig.DEFAULTS.cooldownMillis()));
    }

    @DisplayName("A blank greeting is rejected, naming the field")
    @Test
    void blankGreetingIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> new ExampleConfig("   ", 1_000));

        Assertions.assertEquals("greeting", thrown.field());
    }

    @DisplayName("A greeting without a %s placeholder is rejected, naming the field")
    @Test
    void greetingWithoutPlaceholderIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> new ExampleConfig("Welcome to the lobby!", 1_000));

        Assertions.assertEquals("greeting", thrown.field());
    }

    @DisplayName("A negative cooldown is rejected, naming the field and reason")
    @Test
    void negativeCooldownIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> new ExampleConfig("Hi %s", -1));

        Assertions.assertEquals("cooldownMillis", thrown.field());
        Assertions.assertEquals("must not be negative", thrown.reason());
    }
}
