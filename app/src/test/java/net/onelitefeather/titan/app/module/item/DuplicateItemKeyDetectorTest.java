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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain-Java coverage for {@link DuplicateItemKeyDetector}: it needs no {@link LobbyItem}, no
 * {@link ItemRegistry} and no server, only claims on a key - mirrors
 * {@link SlotConflictDetectorTest}.
 */
class DuplicateItemKeyDetectorTest {

    private final DuplicateItemKeyDetector detector = new DuplicateItemKeyDetector();

    @DisplayName("Distinct keys never conflict")
    @Test
    void distinctKeysNeverConflict() {
        List<DuplicateItemKeyDetector.Claim> claims = List.of(new DuplicateItemKeyDetector.Claim("navigator", "titan:navigator"), new DuplicateItemKeyDetector.Claim("friends", "titan:friends"));

        Assertions.assertEquals(Optional.empty(), this.detector.findConflict(claims));
    }

    @DisplayName("Two modules claiming the same key conflict, naming the key and both modules")
    @Test
    void twoModulesClaimingTheSameKeyConflict() {
        List<DuplicateItemKeyDetector.Claim> claims = List.of(new DuplicateItemKeyDetector.Claim("navigator", "titan:navigator"), new DuplicateItemKeyDetector.Claim("friends", "titan:navigator"));

        Optional<DuplicateItemKeyDetector.Conflict> conflict = this.detector.findConflict(claims);

        Assertions.assertTrue(conflict.isPresent());
        Assertions.assertEquals("titan:navigator", conflict.get().key());
        Assertions.assertEquals("navigator", conflict.get().firstModuleId());
        Assertions.assertEquals("friends", conflict.get().secondModuleId());
    }

    @DisplayName("The same module registering its own key twice still conflicts")
    @Test
    void theSameModuleRegisteringItsOwnKeyTwiceConflicts() {
        List<DuplicateItemKeyDetector.Claim> claims = List.of(new DuplicateItemKeyDetector.Claim("navigator", "titan:navigator"), new DuplicateItemKeyDetector.Claim("navigator", "titan:navigator"));

        Optional<DuplicateItemKeyDetector.Conflict> conflict = this.detector.findConflict(claims);

        Assertions.assertTrue(conflict.isPresent());
        Assertions.assertEquals("navigator", conflict.get().firstModuleId());
        Assertions.assertEquals("navigator", conflict.get().secondModuleId());
    }

    @DisplayName("Only the first conflict is reported when more than two claims share a key")
    @Test
    void onlyTheFirstConflictIsReported() {
        List<DuplicateItemKeyDetector.Claim> claims = List.of(new DuplicateItemKeyDetector.Claim("a", "titan:x"), new DuplicateItemKeyDetector.Claim("b", "titan:x"), new DuplicateItemKeyDetector.Claim("c", "titan:x"));

        Optional<DuplicateItemKeyDetector.Conflict> conflict = this.detector.findConflict(claims);

        Assertions.assertTrue(conflict.isPresent());
        Assertions.assertEquals("a", conflict.get().firstModuleId());
        Assertions.assertEquals("b", conflict.get().secondModuleId());
    }
}
