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
package net.onelitefeather.titan.app.bootstrap;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ComponentTranslationBootstrap}. {@link
 * ComponentTranslationBootstrap#valueToSet(java.util.function.Function)} is the pure decision the
 * class makes, pulled apart from the real {@code System.getProperty}/{@code System.setProperty}
 * calls precisely so a test can supply a fake property lookup (a plain {@link Map}) instead of
 * mutating the real, process-wide system properties (F.I.R.S.T. - Independent/Repeatable; no
 * {@code System.setProperty} in tests).
 */
class ComponentTranslationBootstrapTest {

    @DisplayName("No operator value present: the property must be set to true")
    @Test
    void enablesTranslationWhenNoOperatorValueIsPresent() {
        Map<String, String> properties = Map.of();

        String value = ComponentTranslationBootstrap.valueToSet(properties::get);

        Assertions.assertEquals("true", value);
    }

    @DisplayName("An operator already set the property: it must be left untouched")
    @Test
    void leavesAnExistingOperatorValueUntouched() {
        Map<String, String> properties = Map.of(ComponentTranslationBootstrap.PROPERTY_NAME, "false");

        String value = ComponentTranslationBootstrap.valueToSet(properties::get);

        Assertions.assertNull(value, "an operator-supplied value must never be overwritten");
    }

    @DisplayName("An operator explicitly set it to true: it must still be left untouched")
    @Test
    void leavesAnExistingTrueOperatorValueUntouched() {
        Map<String, String> properties = Map.of(ComponentTranslationBootstrap.PROPERTY_NAME, "true");

        String value = ComponentTranslationBootstrap.valueToSet(properties::get);

        Assertions.assertNull(value);
    }
}
