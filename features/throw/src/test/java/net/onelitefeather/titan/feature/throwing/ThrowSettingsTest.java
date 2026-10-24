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
package net.onelitefeather.titan.feature.throwing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrowSettingsTest {

    @DisplayName("The throw option is named titan:throw.target")
    @Test
    void optionKeyIsTitanThrowTarget() {
        Assertions.assertEquals("titan:throw.target", ThrowSettings.TARGET.key().toString(), "the option key is part of the stored settings and must not change");
    }

    @DisplayName("Being thrown is on by default: the option is opt-out")
    @Test
    void optionIsOnByDefault() {
        Assertions.assertTrue(ThrowSettings.TARGET.defaultValue(), "a player who never chose must be throwable");
    }
}
