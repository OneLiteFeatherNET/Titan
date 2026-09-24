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
package net.onelitefeather.titan.app.module.item;

import java.util.List;
import java.util.Optional;
import net.minestom.server.entity.EquipmentSlot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain-Java coverage for {@link SlotConflictDetector}: it needs no {@link LobbyItem}, no
 * {@link ItemRegistry} and no server, only claims and placements.
 */
class SlotConflictDetectorTest {

    private final SlotConflictDetector detector = new SlotConflictDetector();

    @DisplayName("Distinct hotbar slots never conflict")
    @Test
    void distinctHotbarSlotsNeverConflict() {
        List<SlotConflictDetector.Claim> claims = List.of(new SlotConflictDetector.Claim("navigator", ItemSlot.hotbar(4)), new SlotConflictDetector.Claim("friends", ItemSlot.hotbar(5)));

        Assertions.assertEquals(Optional.empty(), this.detector.findConflict(claims));
    }

    @DisplayName("Two modules claiming hotbar slot 4 conflict, naming the slot and both modules")
    @Test
    void twoModulesClaimingTheSameHotbarSlotConflict() {
        List<SlotConflictDetector.Claim> claims = List.of(new SlotConflictDetector.Claim("navigator", ItemSlot.hotbar(4)), new SlotConflictDetector.Claim("friends", ItemSlot.hotbar(4)));

        Optional<SlotConflictDetector.Conflict> conflict = this.detector.findConflict(claims);

        Assertions.assertTrue(conflict.isPresent());
        Assertions.assertEquals(ItemSlot.hotbar(4), conflict.get().placement());
        Assertions.assertEquals("navigator", conflict.get().firstModuleId());
        Assertions.assertEquals("friends", conflict.get().secondModuleId());
    }

    @DisplayName("Two modules claiming the same equipment slot conflict")
    @Test
    void twoModulesClaimingTheSameEquipmentSlotConflict() {
        List<SlotConflictDetector.Claim> claims = List.of(new SlotConflictDetector.Claim("spawn", ItemSlot.equipment(EquipmentSlot.CHESTPLATE)), new SlotConflictDetector.Claim("cosmetics", ItemSlot.equipment(EquipmentSlot.CHESTPLATE)));

        Optional<SlotConflictDetector.Conflict> conflict = this.detector.findConflict(claims);

        Assertions.assertTrue(conflict.isPresent());
        Assertions.assertEquals("spawn", conflict.get().firstModuleId());
        Assertions.assertEquals("cosmetics", conflict.get().secondModuleId());
    }

    @DisplayName("Unplaced claims never conflict, even with each other")
    @Test
    void unplacedClaimsNeverConflict() {
        List<SlotConflictDetector.Claim> claims = List.of(new SlotConflictDetector.Claim("elytra", ItemSlot.unplaced()), new SlotConflictDetector.Claim("other", ItemSlot.unplaced()));

        Assertions.assertEquals(Optional.empty(), this.detector.findConflict(claims));
    }
}
