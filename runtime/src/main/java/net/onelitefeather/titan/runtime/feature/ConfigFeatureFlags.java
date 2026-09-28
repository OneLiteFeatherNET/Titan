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
package net.onelitefeather.titan.runtime.feature;

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.onelitefeather.titan.core.feature.FeatureFlags;

/**
 * The production {@link FeatureFlags}: a flag is a plain configuration value, read like any other
 * key, with the same sources and override order. {@code known} names the flags this source
 * recognizes and {@code active} decides whether one is on, so this class is trivially testable
 * with plain fakes; {@link #fromClasspathDefaults()} wires both to the real config facade.
 */
public final class ConfigFeatureFlags implements FeatureFlags {

    // The lobby's own shipped application.yaml, never the operator's working-directory file.
    private static final String DEFAULT_RESOURCE = "application.yaml";

    private static final String SECTION = "features";

    private final Set<String> known;
    private final Predicate<String> active;

    public ConfigFeatureFlags(Set<String> known, Predicate<String> active) {
        this.known = Set.copyOf(Objects.requireNonNull(known, "known must not be null"));
        this.active = Objects.requireNonNull(active, "active must not be null");
    }

    @Override
    public boolean exists(String featureName) {
        return this.known.contains(featureName);
    }

    @Override
    public boolean isActive(String featureName) {
        return this.known.contains(featureName) && this.active.test(featureName);
    }

    /**
     * Wires a {@link ConfigFeatureFlags} to the real {@code io.avaje.config.Config} facade: known
     * flags come from the shipped {@value #DEFAULT_RESOURCE} on the classpath, activation from the
     * facade.
     */
    public static ConfigFeatureFlags fromClasspathDefaults() {
        return fromClasspathDefaults(DEFAULT_RESOURCE, ConfigFeatureFlags.class.getClassLoader());
    }

    /**
     * Same as {@link #fromClasspathDefaults()}, but with the classpath resource and class loader
     * given explicitly - the seam a test uses to point at a fixture resource instead.
     */
    public static ConfigFeatureFlags fromClasspathDefaults(String classpathResource, ClassLoader classLoader) {
        Set<String> known = knownFlagsIn(classpathResource, classLoader);
        return new ConfigFeatureFlags(known, name -> Config.getBool(SECTION + "." + name, false));
    }

    // Its own independent Configuration instance, bypassing avaje-config, so no profile, external
    // file, environment variable or system property can add, remove or shadow a "known" flag.
    static Set<String> knownFlagsIn(String classpathResource, ClassLoader classLoader) {
        Objects.requireNonNull(classpathResource, "classpathResource must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Configuration resourceOnly = Configuration.builder().resourceLoader(classLoader::getResourceAsStream).load(classpathResource).build();
        return resourceOnly.forPath(SECTION).keys();
    }
}
