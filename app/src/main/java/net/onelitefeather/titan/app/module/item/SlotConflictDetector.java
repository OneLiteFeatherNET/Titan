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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure conflict detection for {@link ItemSlot} placements: given every module's claim on a
 * placement, in registration order, finds the first placement two different modules both claimed.
 *
 * <p>Kept free of {@link ItemRegistry}, {@link LobbyItem} and everything else that needs a running
 * server, so the rule itself - "first claim wins, second claim on the same placement is a conflict,
 * {@link ItemSlot.Unplaced} never conflicts" - is testable as plain data in, data out.
 */
final class SlotConflictDetector {

    /**
     * One module's claim on a placement.
     *
     * @param moduleId  the claiming module's id
     * @param placement the placement it claims
     */
    record Claim(String moduleId, ItemSlot placement) {
    }

    /**
     * Two modules claiming the same placement.
     *
     * @param placement      the contested placement
     * @param firstModuleId  the module that claimed it first
     * @param secondModuleId the module that claimed it again
     */
    record Conflict(ItemSlot placement, String firstModuleId, String secondModuleId) {
    }

    /**
     * @param claims every registered item's claim, in registration order
     * @return the first conflict found, or empty if every placed claim is unique
     */
    Optional<Conflict> findConflict(List<Claim> claims) {
        Map<ItemSlot, String> claimedBy = new LinkedHashMap<>();
        for (Claim claim : claims) {
            if (claim.placement() instanceof ItemSlot.Unplaced) {
                continue;
            }
            String previousModuleId = claimedBy.putIfAbsent(claim.placement(), claim.moduleId());
            if (previousModuleId != null) {
                return Optional.of(new Conflict(claim.placement(), previousModuleId, claim.moduleId()));
            }
        }
        return Optional.empty();
    }
}
