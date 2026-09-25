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
package net.onelitefeather.titan.common.config;

import io.avaje.config.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code lobby-module-config} spec scenarios "negative duration" and "impossible
 * height bounds" against {@link ConfigSections}: a value that fails a config record's own
 * validation (its compact constructor throwing {@link ConfigException#invalid(String, String)})
 * must abort with a message naming the module, the field and the reason, no matter whether the
 * value came from {@code application.yaml}, a profile or an override.
 */
class ConfigSectionsValidationTest {

    @Test
    @DisplayName("A negative duration is rejected, naming the module, field and reason")
    void negativeDurationIsRejected() {
        Configuration configuration = Configuration.builder().putAll(Map.of("tickle.cooldownMillis", "-5")).build();
        ConfigSections sections = new ConfigSections(configuration);

        ConfigException exception = assertThrows(ConfigException.class, () -> sections.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS));

        assertEquals("tickle", exception.section());
        assertEquals("cooldownMillis", exception.field());
        assertTrue(exception.reason() != null && exception.reason().contains("negative"), "reason should explain why the value is invalid, was: " + exception.reason());
        assertTrue(exception.getMessage().contains("tickle.cooldownMillis"), "message should name module and field: " + exception.getMessage());
    }

    @Test
    @DisplayName("Impossible height bounds are rejected, naming both fields")
    void impossibleHeightBoundsAreRejected() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "spawn.minHeight", "500", "spawn.maxHeight", "-64", "spawn.simulationDistance", "2"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        ConfigException exception = assertThrows(ConfigException.class, () -> sections.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS));

        assertEquals("spawn", exception.section());
        assertEquals("minHeight", exception.field());
        assertTrue(exception.reason() != null && exception.reason().contains("maxHeight"), "reason should name both fields: " + exception.reason());
    }
}
