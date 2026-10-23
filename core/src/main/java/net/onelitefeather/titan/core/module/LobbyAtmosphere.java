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

import java.util.Arrays;
import java.util.Optional;

/**
 * The atmospheres a lobby can wear, by the key a configuration names them with. The values
 * themselves live with the dimension that carries them.
 */
public enum LobbyAtmosphere {

    /** The fog of Slender's waiting lobby. */
    SLENDER_FOG("slender-fog");

    private final String key;

    LobbyAtmosphere(String key) {
        this.key = key;
    }

    /** @return the configuration value that selects this atmosphere */
    public String key() {
        return this.key;
    }

    /**
     * @param key the configuration value, exactly as written
     * @return the atmosphere the key selects, or empty for an unknown key
     */
    public static Optional<LobbyAtmosphere> fromKey(String key) {
        return Arrays.stream(values()).filter(atmosphere -> atmosphere.key.equals(key)).findFirst();
    }
}
