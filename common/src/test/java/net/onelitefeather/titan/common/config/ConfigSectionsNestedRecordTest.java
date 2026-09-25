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
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link ConfigSections} rebuilding a nested record from dotted keys, e.g. {@code
 * sit.offset.x}, {@code sit.offset.y}, {@code sit.offset.z} - the flattening {@code avaje-config}
 * itself performs on a nested YAML map, confirmed empirically in {@code design.md} decision 2.
 */
class ConfigSectionsNestedRecordTest {

    @Test
    @DisplayName("A nested record is rebuilt from its dotted keys")
    void nestedRecordReadsAllFieldsFromDottedKeys() {
        Configuration configuration = Configuration.builder().putAll(Map.of(
                "sit.offset.x", "1.5", "sit.offset.y", "2.25", "sit.offset.z", "3.5"
        )).build();
        ConfigSections sections = new ConfigSections(configuration);

        SitTestConfig sit = sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);

        assertEquals(new Vec(1.5, 2.25, 3.5), sit.offset());
    }

    @Test
    @DisplayName("A field missing from a present nested record falls back to its default")
    void missingNestedFieldFallsBackToDefault() {
        Configuration configuration = Configuration.builder().putAll(Map.of("sit.offset.y", "9.0")).build();
        ConfigSections sections = new ConfigSections(configuration);

        SitTestConfig sit = sections.section("sit", SitTestConfig.class, SitTestConfig.DEFAULTS);

        assertEquals(SitTestConfig.DEFAULTS.offset().x(), sit.offset().x(), "the missing x must default");
        assertEquals(9.0, sit.offset().y(), "the present y must be read from the configuration");
        assertEquals(SitTestConfig.DEFAULTS.offset().z(), sit.offset().z(), "the missing z must default");
    }
}
