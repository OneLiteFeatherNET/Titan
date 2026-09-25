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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the requirement that a duplicate item key's message names the contested key and both
 * modules - mirrors {@link ItemPlacementConflictExceptionTest}.
 */
class DuplicateItemKeyExceptionTest {

    @DisplayName("The message names the contested key and both modules")
    @Test
    void messageNamesTheContestedKeyAndBothModules() {
        DuplicateItemKeyDetector.Conflict conflict = new DuplicateItemKeyDetector.Conflict("titan:navigator", "navigator", "friends");

        DuplicateItemKeyException exception = new DuplicateItemKeyException(conflict);

        Assertions.assertTrue(exception.getMessage().contains("navigator"), "must name the first module");
        Assertions.assertTrue(exception.getMessage().contains("friends"), "must name the second module");
        Assertions.assertTrue(exception.getMessage().contains("titan:navigator"), "must name the contested key");
    }
}
