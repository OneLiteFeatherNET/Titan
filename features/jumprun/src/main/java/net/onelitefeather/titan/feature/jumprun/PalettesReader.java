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
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the palettes from the config: strictly once at start, then whenever a run starts, so an
 * edited file takes effect without a restart.
 */
final class PalettesReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PalettesReader.class);

    private final Configuration config;
    private final Set<String> warned = new HashSet<>();
    private Palettes lastValid;

    PalettesReader(Configuration config) {
        this.config = config;
    }

    /**
     * @throws IllegalArgumentException naming the invalid key and the reason, which aborts the
     *                                  start
     */
    synchronized Palettes readAtStartup() {
        this.lastValid = JumprunSettings.palettes(this.config);
        return this.lastValid;
    }

    /**
     * The current palettes, or the last valid ones while the config is invalid; each problem is
     * warned about once.
     */
    synchronized Palettes current() {
        try {
            this.lastValid = JumprunSettings.palettes(this.config);
            this.warned.clear();
        } catch (IllegalArgumentException e) {
            if (this.lastValid == null) {
                throw e;
            }
            if (this.warned.add(e.getMessage())) {
                LOGGER.warn("Invalid jumprun palettes, keeping the last valid ones: {}", e.getMessage());
            }
        }
        return this.lastValid;
    }
}
