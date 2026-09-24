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
 * Pure conflict detection for {@link LobbyItem#key()}s: given every registration attempt, in
 * registration order, finds the first key two different registrations both claimed.
 *
 * <p>Mirrors {@link SlotConflictDetector} - kept free of {@link ItemRegistry} and everything else
 * that needs a running server, so the rule itself - "first registration wins, a later registration
 * for the same key is a conflict" - is testable as plain data in, data out.
 */
final class DuplicateItemKeyDetector {

    /**
     * One registration's claim on a key.
     *
     * @param moduleId the registering module's id
     * @param key      the claimed key, as {@link net.kyori.adventure.key.Key#asString()}
     */
    record Claim(String moduleId, String key) {
    }

    /**
     * Two registrations claiming the same key.
     *
     * @param key            the contested key
     * @param firstModuleId  the module that claimed it first
     * @param secondModuleId the module that claimed it again
     */
    record Conflict(String key, String firstModuleId, String secondModuleId) {
    }

    /**
     * @param claims every registration attempt's claim, in registration order
     * @return the first conflict found, or empty if every claimed key is unique
     */
    Optional<Conflict> findConflict(List<Claim> claims) {
        Map<String, String> claimedBy = new LinkedHashMap<>();
        for (Claim claim : claims) {
            String previousModuleId = claimedBy.putIfAbsent(claim.key(), claim.moduleId());
            if (previousModuleId != null) {
                return Optional.of(new Conflict(claim.key(), previousModuleId, claim.moduleId()));
            }
        }
        return Optional.empty();
    }
}
