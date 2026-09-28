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
 * This build's variant name and the columns {@code titan.app-variant} shipped it with, read from
 * the generated classpath resource {@value #RESOURCE}.
 */
public final class VariantDescriptor {

    private static final String RESOURCE = "META-INF/titan/variant.properties";

    private final String name;
    private final List<String> columns;

    private VariantDescriptor(String name, List<String> columns) {
        this.name = name;
        this.columns = List.copyOf(columns);
    }

    /**
     * Builds a descriptor directly - the seam a test uses for a synthetic (e.g. missing) column.
     */
    public static VariantDescriptor of(String name, List<String> columns) {
        return new VariantDescriptor(Objects.requireNonNull(name, "name must not be null"), columns);
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
            String columnsRaw = properties.getProperty("columns", "");
            List<String> columns = columnsRaw.isBlank() ? List.of() : Arrays.stream(columnsRaw.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
            return Optional.of(new VariantDescriptor(name, columns));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + RESOURCE, exception);
        }
    }

    public String name() {
        return this.name;
    }

    public List<String> columns() {
        return this.columns;
    }
}
