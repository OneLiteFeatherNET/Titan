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
 * Covers {@link ConfigSections} rebuilding a section's {@code JsonObject} tree from flat keys
 * (see {@link ConfigSections#buildTree}) when one key sets a scalar value for a path and another
 * key sets a nested value under that same path at once, e.g. {@code sit.offset} (a plain value)
 * together with {@code sit.offset.x} (a nested field) - which can happen when a profile overrides
 * a field with a plain value while the base configuration still has it as a nested record.
 * Without a check, which of the two keys "wins" depends on the iteration order of {@link
 * Configuration#forPath(String)}'s key set, silently dropping either the scalar or the nested
 * fields; the fix must fail loudly and the same way regardless of that order.
 */
class ConfigSectionsConflictingKeysTest {

    @Test
    @DisplayName("A scalar key colliding with a nested key under the same path fails clearly, naming section and field")
    void scalarCollidingWithNestedKeyFailsClearly() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "sit.offset", "1.0", "sit.offset.x", "0.5"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        ConfigException exception = assertThrows(ConfigException.class, () -> sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS));

        assertEquals("sit", exception.section());
        assertEquals("offset", exception.field(), "the conflicting field path should be named");
        assertTrue(exception.getMessage().contains("sit.offset"), "message should name section and field: " + exception.getMessage());
    }

    @Test
    @DisplayName("A nested key colliding with a scalar key under the same path fails the same way, regardless of which key is seen first")
    void nestedCollidingWithScalarKeyFailsTheSameWay() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "sit.offset.y", "2.0", "sit.offset", "9.0"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        ConfigException exception = assertThrows(ConfigException.class, () -> sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS));

        assertEquals("sit", exception.section());
        assertEquals("offset", exception.field(), "the conflicting field path should be named the same way regardless of key order");
    }
}
