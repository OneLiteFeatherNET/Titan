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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for {@link SitSettings}: no {@code Config}, no {@code ConfigSections}, no
 * server needed - just the pure validation functions.
 */
class SitSettingsTest {

    @DisplayName("A valid offset is returned unchanged")
    @Test
    void aValidOffsetIsReturnedUnchanged() {
        Vec offset = new Vec(0.5, 0.25, 0.5);
        Assertions.assertEquals(offset, SitSettings.offset(offset));
    }

    @DisplayName("A null offset is rejected, naming the full key")
    @Test
    void nullOffsetIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SitSettings.offset(null));
        Assertions.assertEquals("sit.offset", thrown.field());
    }

    @DisplayName("A valid allowedBlocks list is returned unchanged")
    @Test
    void aValidAllowedBlocksListIsReturnedUnchanged() {
        List<Key> allowedBlocks = List.of(Key.key("minecraft:spruce_stairs"));
        Assertions.assertEquals(allowedBlocks, SitSettings.allowedBlocks(allowedBlocks));
    }

    @DisplayName("A null allowedBlocks list is rejected, naming the full key")
    @Test
    void nullAllowedBlocksIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SitSettings.allowedBlocks(null));
        Assertions.assertEquals("sit.allowedBlocks", thrown.field());
    }

    @DisplayName("An empty allowedBlocks list is rejected, naming the full key")
    @Test
    void emptyAllowedBlocksIsRejected() {
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> SitSettings.allowedBlocks(List.of()));
        Assertions.assertEquals("sit.allowedBlocks", thrown.field());
    }
}
