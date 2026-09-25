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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for {@link SitModule}'s own id and the pure allowed-block rule behind its
 * {@code PlayerBlockInteractEvent} listener ({@link SitModule#isAllowedBlock}). No {@code Env}
 * needed - none of this touches a player, block or instance.
 */
class SitModuleTest {

    @DisplayName("The module id is 'sit'")
    @Test
    void idIsSit() {
        Assertions.assertEquals("sit", new SitModule().id());
    }

    @DisplayName("A block whose key is in the allowed list is allowed")
    @Test
    void allowsAConfiguredBlock() {
        List<Key> allowedBlocks = List.of(Key.key("minecraft:spruce_stairs"), Key.key("minecraft:oak_stairs"));

        Assertions.assertTrue(SitModule.isAllowedBlock(allowedBlocks, Key.key("minecraft:oak_stairs")));
    }

    @DisplayName("A block whose key is not in the allowed list is rejected")
    @Test
    void rejectsAnUnconfiguredBlock() {
        List<Key> allowedBlocks = List.of(Key.key("minecraft:spruce_stairs"));

        Assertions.assertFalse(SitModule.isAllowedBlock(allowedBlocks, Key.key("minecraft:stone")));
    }

    @DisplayName("An empty allowed list rejects every block")
    @Test
    void emptyListRejectsEveryBlock() {
        Assertions.assertFalse(SitModule.isAllowedBlock(List.of(), Key.key("minecraft:spruce_stairs")));
    }
}
