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
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

/**
 * One destination shown in the shared navigator inventories, fixed in code as a Java {@code enum}
 * since destinations change rarely. The minigames ({@code ELYTRA_RACE}, {@code SLENDER}) sit in row
 * one of the grid, the servers in row two. {@link #SLENDER} and {@link #BUILD} are each gated
 * behind
 * a permission; the others are always visible.
 */
enum Destination {

    ELYTRA_RACE(11, Material.ELYTRA, "<gradient:#fcba03:#03fc8c>", "ElytraRace", "ElytraRace", null), SLENDER(15, Material.ENDERMAN_SPAWN_EGG, "<gradient:#616161:#e80000>", "Slender", "Slender", "titan.navigator.slender"), SURVIVAL(20, Material.GRASS_BLOCK, "<gradient:#7CFC00:#00C853>", "Survival", "Survival", null), CREATIVE(22, Material.WOODEN_AXE, "<gradient:#ff6ec7:#7873f5>", "Creative", "MemberBuild", null), BUILD(24, Material.SCAFFOLDING, "<gradient:#FFD700:#FF8C00>", "Build", "Team-Build", "titan.navigator.buildserver");

    private final int slot;
    private final Material icon;
    private final String gradient;
    private final String label;
    private final String task;
    private final @Nullable String permission;

    Destination(int slot, Material icon, String gradient, String label, String task, @Nullable String permission) {
        this.slot = slot;
        this.icon = icon;
        this.gradient = gradient;
        this.label = label;
        this.task = task;
        this.permission = permission;
    }

    int slot() {
        return this.slot;
    }

    /**
     * The MiniMessage gradient opening tag of this destination; its colours style the name,
     * separators and picker entries.
     */
    String gradient() {
        return this.gradient;
    }

    String task() {
        return this.task;
    }

    @Nullable
    String permission() {
        return this.permission;
    }

    /**
     * The entry with its gradient name and the given lore. Rebuilt on every call, so no two callers
     * share a stack.
     */
    ItemStack item(List<Component> lore) {
        Component name = MiniMessage.miniMessage().deserialize("<!i>" + this.gradient + this.label + "</gradient>");
        return ItemStack.builder(this.icon).customName(name).lore(lore).build();
    }

    static List<Destination> permissioned() {
        return Arrays.stream(values()).filter(destination -> destination.permission != null).toList();
    }

    // granted holds the permissioned destinations a menu shows; the others are always shown.
    static List<Destination> visible(Set<Destination> granted) {
        return Arrays.stream(values()).filter(destination -> destination.permission == null || granted.contains(destination)).toList();
    }
}
