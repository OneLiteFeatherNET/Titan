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
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link NavigatorVisibility#visible}: an entry with no feature is always shown,
 * an entry with a feature is shown only while that feature is active, and toggling a feature
 * changes
 * the result of the very next call - covering the "flag off/on/absent" scenarios from
 * {@code openspec/changes/lobby-feature-modules/design.md}, decision 13, at the lowest level of the
 * test pyramid, with no {@code Env} and no Aves.
 */
class NavigatorVisibilityTest {

    private static NavigatorEntry entry(int slot, String destination, String feature) {
        return new NavigatorEntry(slot, ItemStack.of(Material.FEATHER), Component.text(destination), destination, feature);
    }

    private static NavigatorEntry entry(int slot, String destination) {
        return new NavigatorEntry(slot, ItemStack.of(Material.FEATHER), Component.text(destination), destination);
    }

    @DisplayName("An entry with no feature is always visible")
    @Test
    void entryWithNoFeatureIsAlwaysVisible() {
        NavigatorEntry survival = entry(4, "Survival");
        FakeFeatureFlags flags = new FakeFeatureFlags();

        List<NavigatorEntry> visible = NavigatorVisibility.visible(List.of(survival), flags);

        Assertions.assertEquals(List.of(survival), visible);
    }

    @DisplayName("An entry whose feature is off is filtered out")
    @Test
    void entryWithAnInactiveFeatureIsHidden() {
        NavigatorEntry slender = entry(5, "cygnus", "NAVIGATOR_SLENDER");
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", false);

        List<NavigatorEntry> visible = NavigatorVisibility.visible(List.of(slender), flags);

        Assertions.assertTrue(visible.isEmpty());
    }

    @DisplayName("An entry whose feature is on is shown")
    @Test
    void entryWithAnActiveFeatureIsShown() {
        NavigatorEntry slender = entry(5, "cygnus", "NAVIGATOR_SLENDER");
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);

        List<NavigatorEntry> visible = NavigatorVisibility.visible(List.of(slender), flags);

        Assertions.assertEquals(List.of(slender), visible);
    }

    @DisplayName("Toggling a feature between two calls changes the result")
    @Test
    void togglingAFeatureChangesTheResult() {
        NavigatorEntry slender = entry(5, "cygnus", "NAVIGATOR_SLENDER");
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", false);

        Assertions.assertTrue(NavigatorVisibility.visible(List.of(slender), flags).isEmpty());

        flags.set("NAVIGATOR_SLENDER", true);

        Assertions.assertEquals(List.of(slender), NavigatorVisibility.visible(List.of(slender), flags));
    }

    @DisplayName("Entries with and without a feature can be mixed, preserving order")
    @Test
    void mixedEntriesPreserveOrder() {
        NavigatorEntry elytra = entry(0, "ElytraRace");
        NavigatorEntry slender = entry(5, "cygnus", "NAVIGATOR_SLENDER");
        NavigatorEntry creative = entry(8, "MemberBuild");
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);

        List<NavigatorEntry> visible = NavigatorVisibility.visible(List.of(elytra, slender, creative), flags);

        Assertions.assertEquals(List.of(elytra, slender, creative), visible);
    }
}
