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
import net.onelitefeather.titan.core.feature.FeatureFlags;
import org.jetbrains.annotations.Nullable;

/**
 * One destination shown in the shared navigator inventory, fixed in code as a Java {@code enum}
 * since destinations change rarely. Only {@link #SLENDER} is gated behind a feature flag; the
 * other three are always visible.
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

    int slot() {
        return this.slot;
    }

    String task() {
        return this.task;
    }

    @Nullable
    String feature() {
        return this.feature;
    }

    // Rebuilds the icon on every call so callers never share one ItemStack instance.
    ItemStack item() {
        return ItemStack.builder(this.icon).customName(MiniMessage.miniMessage().deserialize(this.displayName)).build();
    }

    static List<Destination> visible(FeatureFlags featureFlags) {
        return Arrays.stream(values()).filter(destination -> destination.feature == null || featureFlags.isActive(destination.feature)).toList();
    }
}
