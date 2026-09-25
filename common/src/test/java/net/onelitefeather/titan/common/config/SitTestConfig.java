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

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;

import java.util.List;

/**
 * A stand-in for the real {@code sit} module's config record, used to exercise {@link
 * SectionBinder}'s {@link Vec} and {@link Key} adapters without depending on the {@code app}
 * module.
 */
record SitTestConfig(Vec offset, List<Key> allowedBlocks) {

    static final SitTestConfig DEFAULTS = new SitTestConfig(new Vec(0.5, 0.25, 0.5), List.of(Key.key("minecraft:spruce_stairs")));

    SitTestConfig {
        if (offset == null) {
            throw ConfigException.invalid("offset", "must not be null");
        }
        allowedBlocks = allowedBlocks == null ? List.of() : List.copyOf(allowedBlocks);
    }
}
