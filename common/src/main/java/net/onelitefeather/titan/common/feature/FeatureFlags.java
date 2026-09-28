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
 * currently on - the {@code features} section of the configuration in production, a fake in a
 * test. A module asks for a flag by name through this interface instead of a global static,
 * keeping its tests fast, independent and repeatable.
 */
public interface FeatureFlags {

    /** @return {@code true} if {@code featureName} is a flag this source knows about, on or off. */
    boolean exists(String featureName);

    /**
     * @return {@code true} if the flag is known and on; {@code false} both when it is off and when
     *         it is unknown - use {@link #exists(String)} to tell those apart.
     */
    boolean isActive(String featureName);
}
