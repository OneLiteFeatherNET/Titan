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
package net.onelitefeather.titan.runtime.variant;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit coverage for {@link ExpectedModules#missingModules(List, java.util.Collection)}. */
class ExpectedModulesTest {

    @DisplayName("Every expected module loaded yields an empty list")
    @Test
    void allPresentYieldsEmptyList() {
        List<String> missing = ExpectedModules.missingModules(List.of("adminColumn", "sitColumn", "spawnColumn"), List.of("adminColumn", "sitColumn", "spawnColumn"));

        Assertions.assertEquals(List.of(), missing);
    }

    @DisplayName("A missing module is reported, in the order it appears in the expected list")
    @Test
    void missingModulesAreReportedInExpectedOrder() {
        List<String> missing = ExpectedModules.missingModules(List.of("adminColumn", "sitColumn", "spawnColumn"), List.of("spawnColumn"));

        Assertions.assertEquals(List.of("adminColumn", "sitColumn"), missing, "order must follow the expected list, not the (differently ordered) loaded one");
    }

    @DisplayName("An empty expected list yields an empty list regardless of what loaded")
    @Test
    void emptyExpectedYieldsEmptyList() {
        List<String> missing = ExpectedModules.missingModules(List.of(), List.of("adminColumn"));

        Assertions.assertEquals(List.of(), missing);
    }

    @DisplayName("Every module missing is reported in full, in expected order")
    @Test
    void allMissingAreReportedInFull() {
        List<String> missing = ExpectedModules.missingModules(List.of("adminColumn", "sitColumn"), List.of());

        Assertions.assertEquals(List.of("adminColumn", "sitColumn"), missing);
    }
}
