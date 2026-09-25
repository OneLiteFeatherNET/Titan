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
package net.onelitefeather.titan.app.feature.navigator;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Turns the flat keys {@code io.avaje.config.Configuration#forPath(String)} returns for the
 * {@code navigator.entries} section into the set of entry names it describes.
 *
 * <p>Deliberately free of any {@code io.avaje.config.Config}/{@code Configuration} type so it can
 * be unit-tested without touching the facade - see {@code openspec/changes/avaje-config-facade/
 * design.md}, decision 6: reading the section stays {@link NavigatorModule}'s job, this class only
 * turns the already-read keys into names.
 */
final class NavigatorEntryKeys {

    private NavigatorEntryKeys() {
    }

    /**
     * @param relativeKeys the keys under {@code navigator.entries}, relative to that section, e.g.
     *                     {@code survival.slot}, {@code survival.icon}, {@code parkour.slot} - as
     *                     returned by {@code Configuration.forPath("navigator.entries").keys()}
     * @return the distinct entry names named by {@code relativeKeys}' first segment, e.g.
     *         {@code {survival, parkour}} for the example above; iteration order is not meaningful
     */
    static Set<String> names(Set<String> relativeKeys) {
        Objects.requireNonNull(relativeKeys, "relativeKeys must not be null");
        Set<String> names = new LinkedHashSet<>();
        for (String key : relativeKeys) {
            Objects.requireNonNull(key, "relativeKeys must not contain a null key");
            int separator = key.indexOf('.');
            names.add(separator < 0 ? key : key.substring(0, separator));
        }
        return names;
    }
}
