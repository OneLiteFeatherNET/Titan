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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit coverage for {@link VariantDescriptor#fromClasspath(ClassLoader)}. */
class VariantDescriptorTest {

    @DisplayName("A classpath with no variant.properties resolves to empty - e.g. runtime's own tests")
    @Test
    void absentResourceResolvesToEmpty() {
        // runtime is a library, never a variant, so its own classpath ships no variant.properties.
        Optional<VariantDescriptor> descriptor = VariantDescriptor.fromClasspath(getClass().getClassLoader());

        Assertions.assertTrue(descriptor.isEmpty());
    }

    @DisplayName("A variant.properties resource resolves its name and comma-separated columns")
    @Test
    void presentResourceResolvesNameAndColumns(@TempDir Path root) throws IOException {
        Path resourceFile = root.resolve("META-INF").resolve("titan").resolve("variant.properties");
        Files.createDirectories(resourceFile.getParent());
        Files.writeString(resourceFile, "name=cloudnet\ncolumns=admin,sit,spawn\n");

        Optional<VariantDescriptor> descriptor = VariantDescriptor.fromClasspath(isolatedClassLoaderFor(root));

        Assertions.assertTrue(descriptor.isPresent());
        Assertions.assertEquals("cloudnet", descriptor.get().name());
        Assertions.assertEquals(List.of("admin", "sit", "spawn"), descriptor.get().columns());
    }

    @DisplayName("of(...) builds a descriptor directly, for a synthetic (e.g. missing) column in a test")
    @Test
    void ofBuildsADescriptorDirectly() {
        VariantDescriptor descriptor = VariantDescriptor.of("cloudnet", List.of("admin", "ghostColumn"));

        Assertions.assertEquals("cloudnet", descriptor.name());
        Assertions.assertEquals(List.of("admin", "ghostColumn"), descriptor.columns());
    }

    private static URLClassLoader isolatedClassLoaderFor(Path root) {
        try {
            URL rootUrl = root.toUri().toURL();
            // No parent: this loader must see only the fixture, never runtime's own real classpath.
            return new URLClassLoader(new URL[]{rootUrl}, null);
        } catch (MalformedURLException exception) {
            throw new AssertionError(exception);
        }
    }
}
