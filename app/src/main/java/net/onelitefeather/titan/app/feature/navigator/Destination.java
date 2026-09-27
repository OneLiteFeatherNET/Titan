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

import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import org.jetbrains.annotations.Nullable;

/**
 * One destination shown in the shared navigator inventory - fixed in code, per
 * {@code openspec/changes/navigator-entries-in-code/design.md}, decision 2: the navigator's
 * targets change rarely and only alongside a release, so a Java {@code enum} replaces the former
 * {@code navigator.entries} configuration section and the platform-wide entry registry.
 *
 * <p>Slot, icon and display name match the values the lobby shipped in
 * {@code application.yaml} before this change - see the {@code lobby-navigator} spec's
 * "Standardziele" scenario. Only {@link #SLENDER} is gated behind a feature flag; the other three
 * are always visible.
 */
enum Destination {

    ELYTRA_RACE(0, Material.ELYTRA, "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>", "ElytraRace", null), SURVIVAL(4, Material.GRASS_BLOCK, "<!i><green>Survival", "Survival", null), SLENDER(5, Material.ENDERMAN_SPAWN_EGG, "<!i><gradient:#616161:#e80000c>Slender</gradient>", "cygnus", "NAVIGATOR_SLENDER"), CREATIVE(8, Material.WOODEN_AXE, "<!i><rainbow>Creative</rainbow>", "MemberBuild", null);

    private final int slot;
    private final Material icon;
    private final String displayName;
    private final String task;
    private final @Nullable String feature;

    Destination(int slot, Material icon, String displayName, String task, @Nullable String feature) {
        this.slot = slot;
        this.icon = icon;
        this.displayName = displayName;
        this.task = task;
        this.feature = feature;
    }

    /**
     * @return the slot this destination occupies in the shared {@code CHEST_1_ROW} inventory,
     *         {@code 0}-{@code 8}
     */
    int slot() {
        return this.slot;
    }

    /**
     * @return the CloudNet task name a click on this destination delivers the player to
     */
    String task() {
        return this.task;
    }

    /**
     * @return the name of the feature flag this destination is gated behind, or {@code null} if it
     *         is always visible
     */
    @Nullable
    String feature() {
        return this.feature;
    }

    /**
     * @return the built icon, deserialized from {@link #displayName} on every call so a caller
     *         never has to share one {@link ItemStack} instance across players
     */
    ItemStack item() {
        return ItemStack.builder(this.icon).customName(MiniMessage.miniMessage().deserialize(this.displayName)).build();
    }

    /**
     * @param featureFlags the source of truth {@link #feature} is checked against
     * @return every destination with no feature gate, plus every one whose feature is currently
     *         active according to {@code featureFlags}, in declaration order ({@link #slot()}
     *         order)
     */
    static List<Destination> visible(FeatureFlags featureFlags) {
        return Arrays.stream(values()).filter(destination -> destination.feature == null || featureFlags.isActive(destination.feature)).toList();
    }
}
