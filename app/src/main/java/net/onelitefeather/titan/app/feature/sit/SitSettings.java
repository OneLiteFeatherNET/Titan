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
package net.onelitefeather.titan.app.feature.sit;

import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.common.config.ConfigException;

/**
 * Pure validation for the {@code sit} section's values, kept apart from however those values were
 * read (today {@link SitConfig}'s compact constructor, called by {@code SectionBinder}).
 *
 * <p>Every method takes a plain value and either returns it unchanged or throws
 * {@link ConfigException#invalid(String, String)} naming the value's full section key, e.g.
 * {@code sit.offset}. None of these methods touch {@code io.avaje.config.Config},
 * {@code ConfigSections} or a server, so they are unit-testable on their own.
 */
final class SitSettings {

    private SitSettings() {
    }

    /**
     * @param offset the offset from the clicked block's position to the seat entity
     * @return {@code offset}, unchanged
     * @throws ConfigException if {@code offset} is {@code null}
     */
    static Vec offset(Vec offset) {
        if (offset == null) {
            throw ConfigException.invalid("sit.offset", "must not be null");
        }
        return offset;
    }

    /**
     * @param allowedBlocks the block keys a player may sit down on
     * @return {@code allowedBlocks}, unchanged
     * @throws ConfigException if {@code allowedBlocks} is {@code null} or empty - a player could
     *                         never sit down otherwise
     */
    static List<Key> allowedBlocks(List<Key> allowedBlocks) {
        if (allowedBlocks == null) {
            throw ConfigException.invalid("sit.allowedBlocks", "must not be null");
        }
        if (allowedBlocks.isEmpty()) {
            throw ConfigException.invalid("sit.allowedBlocks", "must not be empty - a player could never sit down otherwise");
        }
        return allowedBlocks;
    }
}
