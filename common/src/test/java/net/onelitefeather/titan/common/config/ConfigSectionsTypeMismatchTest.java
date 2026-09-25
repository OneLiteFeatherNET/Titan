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
 * Covers the {@code lobby-module-config} spec scenario "invalid override": a value coming from an
 * override (in {@code avaje-config}'s terms, any value in the {@link Configuration}, since a
 * single flat string is all a profile, an env variable or a system property ever set) that does
 * not match its field's type must be reported the same way a bad value in {@code application.yaml}
 * itself would be, naming the module (section) and the field.
 */
class ConfigSectionsTypeMismatchTest {

    @Test
    @DisplayName("A non-numeric override for a long field is rejected, naming module and field")
    void nonNumericOverrideForLongFieldIsRejected() {
        Configuration configuration = Configuration.builder().putAll(Map.of("tickle.cooldownMillis", "abc")).build();
        ConfigSections sections = new ConfigSections(configuration);

        ConfigException exception = assertThrows(ConfigException.class, () -> sections.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS));

        assertEquals("tickle", exception.section());
        assertEquals("cooldownMillis", exception.field(), "the offending field should be found by checking the record's components");
        assertTrue(exception.reason() != null && !exception.reason().isBlank(), "a type mismatch must carry a reason");
        assertTrue(exception.getMessage().contains("tickle.cooldownMillis"), "message should name section and field: " + exception.getMessage());
    }

    @Test
    @DisplayName("A non-numeric override inside a multi-field section still names the offending field")
    void mismatchInMultiFieldSectionNamesTheField() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "spawn.minHeight", "-64", "spawn.maxHeight", "not-a-number", "spawn.simulationDistance", "2"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        ConfigException exception = assertThrows(ConfigException.class, () -> sections.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS));

        assertEquals("spawn", exception.section());
        assertEquals("maxHeight", exception.field());
    }
}
