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
package net.onelitefeather.titan.feature.navigator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.portal.PlayerCount;
import org.jetbrains.annotations.Nullable;

/**
 * One destination shown in the shared navigator inventories, fixed in code as a Java {@code enum}
 * since destinations change rarely. {@link #SLENDER} is gated behind a feature flag and {@link
 * #BUILD} behind a permission; the others are always visible.
 */
enum Destination {

    ELYTRA_RACE(0, Material.ELYTRA, "<!i><gradient:#00ddff:#ffffff>ElytraRace</gradient>", "ElytraRace", null, null), SURVIVAL(4, Material.GRASS_BLOCK, "<!i><gradient:#3fa9ff:#e0f7ff>Survival</gradient>", "Survival", null, null), SLENDER(5, Material.ENDERMAN_SPAWN_EGG, "<!i><gradient:#00b4d8:#caf0f8>Slender</gradient>", "Slender", Destination.SLENDER_FLAG, null), BUILD(7, Material.SCAFFOLDING, "<!i><gradient:#1e90ff:#bfe6ff>Build</gradient>", "Team-Build", null, "titan.navigator.buildserver"), CREATIVE(8, Material.WOODEN_AXE, "<!i><gradient:#ffffff:#3fa9ff>Creative</gradient>", "MemberBuild", null, null);

    // The only feature flag the navigator knows; navigator.yaml lists it and nothing else.
    static final String SLENDER_FLAG = "NAVIGATOR_SLENDER";

    private static final String COUNT_STYLE = "<gradient:#00ddff:#ffffff>";

    private final int slot;
    private final Material icon;
    private final String displayName;
    private final String task;
    private final @Nullable String feature;
    private final @Nullable String permission;

    Destination(int slot, Material icon, String displayName, String task, @Nullable String feature, @Nullable String permission) {
        this.slot = slot;
        this.icon = icon;
        this.displayName = displayName;
        this.task = task;
        this.feature = feature;
        this.permission = permission;
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

    @Nullable
    String permission() {
        return this.permission;
    }

    // Rebuilds the icon on every call so callers never share one ItemStack instance.
    ItemStack item() {
        return item(Optional.empty());
    }

    // An unknown count adds no line, so it never reads as an empty lobby.
    ItemStack item(Optional<PlayerCount> players) {
        ItemStack.Builder builder = ItemStack.builder(this.icon).customName(MiniMessage.miniMessage().deserialize(this.displayName));
        if (players.isPresent()) {
            builder.lore(playersLine(players.get()));
        }
        return builder.build();
    }

    // The max is 0 when the provider does not know it; then only the current count is shown.
    static Component playersLine(PlayerCount players) {
        String count = COUNT_STYLE + players.online() + "</gradient>";
        if (players.max() > 0) {
            count += "<gray>/</gray>" + COUNT_STYLE + players.max() + "</gradient>";
        }
        return MiniMessage.miniMessage().deserialize("<!i>" + count);
    }

    // withPermissioned is the caller's verdict for a whole menu: the team menu passes true, the
    // public one false.
    static List<Destination> visible(FeatureFlags featureFlags, boolean withPermissioned) {
        return Arrays.stream(values()).filter(destination -> destination.feature == null || featureFlags.isActive(destination.feature)).filter(destination -> destination.permission == null || withPermissioned).toList();
    }
}
