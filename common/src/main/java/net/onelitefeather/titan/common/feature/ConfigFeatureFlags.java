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
package net.onelitefeather.titan.common.feature;

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The production {@link FeatureFlags}: a flag is a plain configuration value, read like any other
 * (see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 4). This replaces
 * the previous, third-party feature-flag library and its own {@code flags.properties} file and
 * classloader workaround; a flag now has the same sources and the same override order as every
 * other configuration key.
 *
 * <p>The constructor takes no dependency on {@code io.avaje.config.Config} at all: {@code known} is
 * the set of flag names this source recognizes, and {@code active} decides whether a given name is
 * currently on. That keeps this class trivially unit-testable with plain fakes - a test never has
 * to
 * touch the static facade or a real {@code application.yaml}. {@link #fromClasspathDefaults()} is
 * the one place production code wires both to the real thing.
 */
public final class ConfigFeatureFlags implements FeatureFlags {

    /**
     * The classpath resource {@link #fromClasspathDefaults()} loads the known flag names from - the
     * lobby's own shipped {@code application.yaml}, never the operator's working-directory file.
     */
    private static final String DEFAULT_RESOURCE = "application.yaml";

    /**
     * The configuration section a flag's key lives under, e.g. {@code features.NAVIGATOR_SLENDER}.
     */
    private static final String SECTION = "features";

    private final Set<String> known;
    private final Predicate<String> active;

    /**
     * @param known  the set of feature names this source recognizes, regardless of whether they are
     *               currently on or off
     * @param active decides whether a given name is currently active; never invoked for a name
     *               {@code known} does not contain, since {@link #isActive(String)} short-circuits
     *               to
     *               {@code false} for those first
     */
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
     * flags come from the keys under {@value #SECTION} in the lobby's own classpath
     * {@value #DEFAULT_RESOURCE} - loaded as its own, independent {@link Configuration} instance,
     * so
     * a typo in the operator's working-directory file, a profile or an override can never make a
     * flag "known" - and a flag's activation reads {@code features.<name>} through the facade,
     * defaulting to {@code false} when unset.
     *
     * @return a {@link ConfigFeatureFlags} backed by the facade and the shipped defaults
     */
    public static ConfigFeatureFlags fromClasspathDefaults() {
        return fromClasspathDefaults(DEFAULT_RESOURCE, ConfigFeatureFlags.class.getClassLoader());
    }

    /**
     * Same as {@link #fromClasspathDefaults()}, but with the classpath resource and class loader
     * given explicitly - the seam a test uses to point at a fixture resource instead of the real
     * {@value #DEFAULT_RESOURCE}, without ever having to touch the static
     * {@code io.avaje.config.Config} facade for the known-flags half of this wiring.
     *
     * @param classpathResource the classpath resource to load the known flags from, e.g.
     *                          {@value #DEFAULT_RESOURCE}
     * @param classLoader       the class loader {@code classpathResource} is resolved against
     * @return a {@link ConfigFeatureFlags} backed by the facade for activation and
     *         {@code classpathResource} for the known flags
     */
    public static ConfigFeatureFlags fromClasspathDefaults(String classpathResource, ClassLoader classLoader) {
        Set<String> known = knownFlagsIn(classpathResource, classLoader);
        return new ConfigFeatureFlags(known, name -> Config.getBool(SECTION + "." + name, false));
    }

    /**
     * Loads {@code classpathResource} as its own, independent {@link Configuration} instance -
     * bypassing the full avaje-config pipeline entirely, so no profile, external file, environment
     * variable or system property can add, remove or shadow a "known" flag - and returns the keys
     * found directly under {@value #SECTION} in it.
     *
     * @param classpathResource the classpath resource to load, e.g. {@value #DEFAULT_RESOURCE}
     * @param classLoader       the class loader {@code classpathResource} is resolved against
     * @return the flag names found under {@value #SECTION} in {@code classpathResource}; empty if
     *         the
     *         resource is not found or has no such section
     */
    static Set<String> knownFlagsIn(String classpathResource, ClassLoader classLoader) {
        Objects.requireNonNull(classpathResource, "classpathResource must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Configuration resourceOnly = Configuration.builder().resourceLoader(classLoader::getResourceAsStream).load(classpathResource).build();
        return resourceOnly.forPath(SECTION).keys();
    }
}
