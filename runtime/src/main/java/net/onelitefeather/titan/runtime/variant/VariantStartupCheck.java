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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aborts the boot when a module the running variant expects did not load (spec {@code
 * app-variants}, "Eine Variante startet nur mit allen erwarteten Columns").
 * {@link net.onelitefeather.titan.runtime.Titan}'s constructor calls this right after the
 * {@link io.avaje.inject.BeanScope} is built, so a feature that failed to register is caught
 * before any player can connect; {@code main} logs the resulting {@link IllegalStateException} at
 * ERROR and exits, exactly as it already does for any other startup failure.
 */
public final class VariantStartupCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariantStartupCheck.class);

    private VariantStartupCheck() {
    }

    /** Skips the check when {@code loader} carries no {@code variant.properties} at all. */
    public static void verify(ClassLoader loader) {
        VariantDescriptor.fromClasspath(loader).ifPresent(descriptor -> verify(descriptor, LoadedModules.discover(loader)));
    }

    /**
     * @throws IllegalStateException naming every module {@code descriptor} expects but
     *                               {@code loadedModules} does not contain
     */
    public static void verify(VariantDescriptor descriptor, List<String> loadedModules) {
        List<String> missing = ExpectedModules.missingModules(descriptor.modules(), loadedModules);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Variant " + descriptor.name() + " is missing modules: " + missing);
        }
        LOGGER.info("Variant {} started with modules {}", descriptor.name(), descriptor.modules());
    }
}
