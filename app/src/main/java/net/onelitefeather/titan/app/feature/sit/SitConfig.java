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

/**
 * The {@code sit} module's own configuration section.
 *
 * <p>{@code offset} is added to the block position a player clicked to place the invisible seat
 * entity; {@code allowedBlocks} lists the block keys (e.g. {@code "minecraft:spruce_stairs"}) a
 * player may sit down on. Both fields are required: a player can never sit at all without at
 * least one allowed block, so - unlike a field that simply falls back to a sensible default - an
 * empty list is rejected rather than silently turning sitting off. An operator who wants to
 * disable the feature disables the whole module instead.
 *
 * <p>Validation itself lives in the pure, package-private {@link SitSettings}; this compact
 * constructor only delegates to it and keeps {@code allowedBlocks} defensively copied.
 *
 * @param offset        the offset from the clicked block's position to the seat entity, e.g.
 *                      {@code (0.5, 0.25, 0.5)} to center it on top of the block
 * @param allowedBlocks the block keys a player may sit down on; must contain at least one entry
 */
public record SitConfig(Vec offset, List<Key> allowedBlocks) {

    /**
     * The documented defaults: a seat centered on top of the clicked block, allowing only
     * {@code minecraft:spruce_stairs} - the same block the lobby has always allowed.
     */
    public static final SitConfig DEFAULTS = new SitConfig(new Vec(0.5, 0.25, 0.5), List.of(Key.key("minecraft:spruce_stairs")));

    public SitConfig {
        offset = SitSettings.offset(offset);
        allowedBlocks = List.copyOf(SitSettings.allowedBlocks(allowedBlocks));
    }
}
