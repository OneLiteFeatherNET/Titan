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
package net.onelitefeather.titan.buildsrc.variant;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Plain-Java unit coverage for {@link PlatformModules}: no Gradle project needed (F.I.R.S.T.). */
class PlatformModulesTest {

    @DisplayName("A single platform name becomes its own expected-module id")
    @Test
    void aSinglePlatformNameBecomesItsExpectedModuleId() {
        List<String> ids = PlatformModules.expectedModuleIdsOf("luckperms");

        Assertions.assertEquals(List.of("luckpermsPlatform"), ids);
    }

    @DisplayName("Several platform names each become their own expected-module id, in order")
    @Test
    void severalPlatformNamesEachBecomeTheirOwnExpectedModuleId() {
        List<String> ids = PlatformModules.expectedModuleIdsOf("luckperms", "example");

        Assertions.assertEquals(List.of("luckpermsPlatform", "examplePlatform"), ids);
    }

    @DisplayName("No platform names yield no expected-module ids")
    @Test
    void noPlatformNamesYieldNoExpectedModuleIds() {
        List<String> ids = PlatformModules.expectedModuleIdsOf();

        Assertions.assertTrue(ids.isEmpty());
    }
}
