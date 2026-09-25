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

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for what is still specific to the {@link SitConfig} record itself - its
 * documented {@link SitConfig#DEFAULTS} and the defensive copy of {@code allowedBlocks} its
 * compact constructor performs. The validation cases now live in {@link SitSettingsTest}.
 */
class SitConfigTest {

    @DisplayName("DEFAULTS matches the documented offset and allowed block")
    @Test
    void defaultsMatchDocumentedValues() {
        Assertions.assertEquals(new Vec(0.5, 0.25, 0.5), SitConfig.DEFAULTS.offset());
        Assertions.assertEquals(List.of(Key.key("minecraft:spruce_stairs")), SitConfig.DEFAULTS.allowedBlocks());
    }

    @DisplayName("allowedBlocks is defensively copied: mutating the source list afterwards has no effect")
    @Test
    void allowedBlocksIsDefensivelyCopied() {
        List<Key> source = new ArrayList<>(List.of(Key.key("minecraft:oak_stairs")));
        SitConfig config = new SitConfig(new Vec(0.5, 0.25, 0.5), source);

        source.add(Key.key("minecraft:spruce_stairs"));

        Assertions.assertEquals(List.of(Key.key("minecraft:oak_stairs")), config.allowedBlocks());
    }

    @DisplayName("allowedBlocks itself is immutable")
    @Test
    void allowedBlocksIsImmutable() {
        SitConfig config = new SitConfig(new Vec(0.5, 0.25, 0.5), List.of(Key.key("minecraft:oak_stairs")));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> config.allowedBlocks().add(Key.key("minecraft:spruce_stairs")));
    }
}
