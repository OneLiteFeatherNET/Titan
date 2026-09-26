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
package net.onelitefeather.titan.common.config;

import io.avaje.config.Configuration;
import java.util.Objects;

/**
 * Loads a classpath resource - always the lobby's own shipped {@code application.yaml}, in
 * practice - as its own, independent {@link Configuration} instance, bypassing the full
 * avaje-config pipeline entirely: no profile, external file, environment variable or system
 * property can add, remove or shadow a value read from the result.
 *
 * <p>Shared by every production seam that needs the lobby's <em>shipped</em> defaults rather than
 * whatever the operator's own configuration currently says - see
 * {@code net.onelitefeather.titan.common.feature.ConfigFeatureFlags#fromClasspathDefaults()} (the
 * known feature-flag names) and {@link RuntimeConfigFallback} (the value substituted for an
 * invalid runtime value) - so the loading itself is written, and can be verified, exactly once
 * (see {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 3.1: "reuse, don't
 * duplicate the loading code").
 */
public final class ClasspathConfiguration {

    private ClasspathConfiguration() {
    }

    /**
     * @param classpathResource the classpath resource to load, e.g. {@code "application.yaml"}
     * @param classLoader       the class loader {@code classpathResource} is resolved against
     * @return a fresh {@link Configuration}, built from {@code classpathResource} alone
     */
    public static Configuration load(String classpathResource, ClassLoader classLoader) {
        Objects.requireNonNull(classpathResource, "classpathResource must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        return Configuration.builder().resourceLoader(classLoader::getResourceAsStream).load(classpathResource).build();
    }
}
