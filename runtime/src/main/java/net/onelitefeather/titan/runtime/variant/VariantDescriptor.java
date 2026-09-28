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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * This build's variant name and the modules {@code titan.app-variant} shipped it with, read from
 * the generated classpath resource {@value #RESOURCE}.
 */
public final class VariantDescriptor {

    private static final String RESOURCE = "META-INF/titan/variant.properties";

    private final String name;
    private final List<String> modules;

    private VariantDescriptor(String name, List<String> modules) {
        this.name = name;
        this.modules = List.copyOf(modules);
    }

    /**
     * Builds a descriptor directly - the seam a test uses for a synthetic (e.g. missing) module.
     */
    public static VariantDescriptor of(String name, List<String> modules) {
        return new VariantDescriptor(Objects.requireNonNull(name, "name must not be null"), modules);
    }

    /**
     * @return the descriptor read from {@value #RESOURCE} on {@code loader}, or empty when the
     *         resource is absent - e.g. {@code runtime}'s own tests, which ship no variant
     */
    public static Optional<VariantDescriptor> fromClasspath(ClassLoader loader) {
        try (InputStream in = loader.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return Optional.empty();
            }
            Properties properties = new Properties();
            properties.load(in);
            String name = properties.getProperty("name", "");
            String modulesRaw = properties.getProperty("modules", "");
            List<String> modules = modulesRaw.isBlank() ? List.of() : Arrays.stream(modulesRaw.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
            return Optional.of(new VariantDescriptor(name, modules));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + RESOURCE, exception);
        }
    }

    public String name() {
        return this.name;
    }

    public List<String> modules() {
        return this.modules;
    }
}
