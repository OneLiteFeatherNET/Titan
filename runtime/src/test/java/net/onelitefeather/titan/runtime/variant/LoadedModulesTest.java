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

/**
 * Unit coverage for {@link LoadedModules#discover(ClassLoader)}, against the real test classpath's
 * {@code META-INF/services/io.avaje.inject.spi.InjectExtension} - see {@link FooColumnModule} and
 * {@link BarPlatformModule}.
 */
class LoadedModulesTest {

    @DisplayName("Strips the Module suffix and lowercases the first letter of a discovered module")
    @Test
    void discoversAndNormalizesAModuleName() {
        List<String> modules = LoadedModules.discover(getClass().getClassLoader());

        Assertions.assertTrue(modules.contains("fooColumn"), "FooColumnModule must resolve to \"fooColumn\"; found: " + modules);
    }

    @DisplayName("The Module suffix stripping is generic, not tied to the Column naming convention")
    @Test
    void discoversAModuleNameThatIsNotAColumn() {
        List<String> modules = LoadedModules.discover(getClass().getClassLoader());

        Assertions.assertTrue(modules.contains("barPlatform"), "BarPlatformModule must resolve to \"barPlatform\"; found: " + modules);
    }
}
