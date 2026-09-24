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
package net.onelitefeather.titan.setup.config;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;

import java.util.List;

/**
 * Setup-local mirror of the lobby's {@code sit} config section (id {@code "sit"}).
 * <p>
 * The setup server does not depend on {@code app}, so it cannot reuse the real feature module's
 * config record; this record duplicates the two fields the {@code app} command edits.
 *
 * @param offset        the offset applied to a sitting player's position
 * @param allowedBlocks the block keys a player may sit on
 */
public record SitSectionConfig(Vec offset, List<Key> allowedBlocks) {

    /**
     * The defaults used when {@code app.json} has no {@code sit} section yet, matching the
     * lobby's own defaults.
     */
    public static final SitSectionConfig DEFAULTS = new SitSectionConfig(new Vec(0.5, 0.25, 0.5), List.of(Material.SPRUCE_STAIRS.key()));

    public SitSectionConfig {
        allowedBlocks = allowedBlocks == null ? List.of() : List.copyOf(allowedBlocks);
    }
}
