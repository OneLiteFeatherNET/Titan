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
package net.onelitefeather.titan.feature.season;

import io.avaje.config.Configuration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.onelitefeather.titan.core.module.LobbyAtmosphere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads {@code seasons.<id>.atmosphere} for the season that is active at startup. Only read at
 * startup, so an unknown value is logged once there and never on the live check.
 */
final class SeasonAtmospheres {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonAtmospheres.class);

    private final Configuration config;

    SeasonAtmospheres(Configuration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * @param seasonId the id of the season, without the {@code seasons.} prefix
     * @return the configured atmosphere; empty if absent, blank or unknown (an unknown value is
     *         logged)
     */
    Optional<LobbyAtmosphere> of(String seasonId) {
        String key = SeasonSettings.key(seasonId, SeasonSettings.ATMOSPHERE_FIELD);
        Optional<String> raw = this.config.getOptional(key).map(String::strip).filter(value -> !value.isEmpty());
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        Optional<LobbyAtmosphere> atmosphere = LobbyAtmosphere.fromKey(raw.get());
        if (atmosphere.isEmpty()) {
            LOGGER.warn("Unknown atmosphere '{}' at {}, the lobby runs without one; known values: {}", raw.get(), key, knownValues());
        }
        return atmosphere;
    }

    private static String knownValues() {
        return Arrays.stream(LobbyAtmosphere.values()).map(LobbyAtmosphere::key).collect(Collectors.joining(", "));
    }
}
