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

/**
 * A small seam between a feature module and whatever technology decides if a named feature flag is
 * currently on - Togglz in production, a fake in a test.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/design.md}, decision 13: a feature module
 * that wants to gate part of its behaviour behind a flag asks for it by name through this
 * interface, injected via its constructor, instead of reaching into the static
 * {@code org.togglz.core.context.FeatureContext} (or a concrete {@code TitanFeatures} enum
 * constant) directly. That keeps the module's tests fast, independent and repeatable - a test hands
 * in a trivial fake instead of a real {@code flags.properties} file and the static Togglz
 * {@code FeatureManager} singleton it implies.
 *
 * <p>This lives in {@code titan.common} rather than inside a feature package: it has no feature
 * of its own, both the composition root ({@code net.onelitefeather.titan.app.Titan}) and any
 * feature module need to reach it, and {@code app.module}/{@code titan.common} classes must never
 * depend on {@code app.feature} (see {@code design.md}, decision 10.2, and
 * {@code ArchitectureTest#platformAndCommonDoNotDependOnFeatures}) - the reverse direction, a
 * feature depending on {@code titan.common}, is exactly what every feature already does for
 * {@link net.onelitefeather.titan.common.config.ConfigException} and friends.
 */
public interface FeatureFlags {

    /**
     * @param featureName the feature's name, e.g. {@code "NAVIGATOR_SLENDER"}
     * @return {@code true} if {@code featureName} names a feature this source knows about at all,
     *         regardless of whether it is currently on or off
     */
    boolean exists(String featureName);

    /**
     * @param featureName the feature's name, e.g. {@code "NAVIGATOR_SLENDER"}
     * @return {@code true} if the named feature is currently active; {@code false} both when it is
     *         switched off and when {@code featureName} is not a feature this source knows about at
     *         all - a caller that must tell the two apart uses {@link #exists(String)} first
     */
    boolean isActive(String featureName);
}
