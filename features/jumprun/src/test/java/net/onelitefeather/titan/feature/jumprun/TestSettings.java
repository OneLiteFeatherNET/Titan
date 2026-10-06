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
package net.onelitefeather.titan.feature.jumprun;

import io.avaje.config.Configuration;
import java.util.Map;
import net.onelitefeather.titan.feature.jumprun.course.Surface;

/** Fixture factory for the shipped jumprun configuration. */
final class TestSettings {

    private static final String SHIPPED_DEFAULTS = "titan/defaults/jumprun.yaml";

    private TestSettings() {
    }

    /**
     * A configuration holding only the shipped defaults, independent of the global one and of
     * what other tests set there. Each call returns a fresh, mutable one.
     */
    static Configuration shippedConfiguration() {
        return Configuration.builder().load(SHIPPED_DEFAULTS).build();
    }

    /** The shipped defaults with the palette of {@code surface} replaced by {@code weights}. */
    static Configuration shippedWith(Surface surface, Map<String, String> weights) {
        Configuration config = shippedConfiguration();
        config.forPath(JumprunSettings.key(surface)).keys().forEach(block -> config.clearProperty(JumprunSettings.key(surface, block)));
        weights.forEach((block, weight) -> config.setProperty(JumprunSettings.key(surface, block), weight));
        return config;
    }

    static JumprunConfig shippedReader() {
        return new JumprunConfig(shippedConfiguration());
    }
}
