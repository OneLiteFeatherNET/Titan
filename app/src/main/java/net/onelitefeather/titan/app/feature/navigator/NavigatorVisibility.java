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

import java.util.List;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.common.feature.FeatureFlags;

/**
 * Which of the registry's entries are currently eligible to show: every entry with no
 * {@link NavigatorEntry#feature()}, plus every entry whose feature is currently active according to
 * a {@link FeatureFlags} source.
 *
 * <p>Kept apart from {@link NavigatorInventory} for the same reason {@link NavigatorLayout} is kept
 * apart from it: this is pure data-in, data-out logic, testable without an Aves
 * {@code GlobalInventoryBuilder} or a Minestom {@code Inventory} at all. See
 * {@code openspec/changes/lobby-feature-modules/design.md}, decision 13.
 */
final class NavigatorVisibility {

    private NavigatorVisibility() {
    }

    /**
     * @param entries      every entry currently in the platform-wide registry, e.g.
     *                     {@code NavigatorEntries#entries()}'s snapshot
     * @param featureFlags the source of truth for whether a named feature is currently active
     * @return {@code entries}, filtered down to the ones eligible to show right now, in the same
     *         order
     */
    static List<NavigatorEntry> visible(List<NavigatorEntry> entries, FeatureFlags featureFlags) {
        return entries.stream().filter(entry -> isVisible(entry, featureFlags)).toList();
    }

    private static boolean isVisible(NavigatorEntry entry, FeatureFlags featureFlags) {
        String feature = entry.feature();
        return feature == null || featureFlags.isActive(feature);
    }
}
