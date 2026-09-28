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
package net.onelitefeather.titan.platform.luckperms;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LuckPermsExtensionCheckTest {

    @DisplayName("A loaded LuckPerms extension aborts with a message naming it")
    @Test
    void aLoadedLuckPermsExtensionAbortsWithAMessageNamingIt() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> LuckPermsExtensionCheck.ensureNotLoadedTwice(List.of("LuckPerms")));

        Assertions.assertTrue(exception.getMessage().contains("LuckPerms"), "the message must name the LuckPerms extension");
    }

    @DisplayName("The LuckPerms extension name is matched case-insensitively")
    @Test
    void theLuckPermsExtensionNameIsMatchedCaseInsensitively() {
        Assertions.assertThrows(IllegalStateException.class, () -> LuckPermsExtensionCheck.ensureNotLoadedTwice(List.of("luckperms")));
    }

    @DisplayName("A list of other extensions passes")
    @Test
    void aListOfOtherExtensionsPasses() {
        Assertions.assertDoesNotThrow(() -> LuckPermsExtensionCheck.ensureNotLoadedTwice(List.of("SomeOtherExtension", "AnotherOne")));
    }

    @DisplayName("An empty list passes")
    @Test
    void anEmptyListPasses() {
        Assertions.assertDoesNotThrow(() -> LuckPermsExtensionCheck.ensureNotLoadedTwice(List.of()));
    }
}
