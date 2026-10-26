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
 * since destinations change rarely. The live entries sit on the main grid. The staging entries
 * sit in the first row of their submenu, slots 0 to 3, and never on the main grid. Every entry but
 * {@link #ELYTRA_RACE}, {@link #SURVIVAL} and {@link #CREATIVE} is gated behind a permission.
 */
enum Destination {

    ELYTRA_RACE(11, Material.ELYTRA, "<gradient:#fcba03:#03fc8c>", "ElytraRace", "ElytraRace", null), SLENDER(15, Material.ENDERMAN_SPAWN_EGG, "<gradient:#616161:#e80000>", "Slender", "Slender", "titan.navigator.slender"), BOUNCE(13, Material.SLIME_BALL, "<gradient:#00FA9A:#40E0D0>", "Bounce", "Bounce", "titan.navigator.bounce"), TAMIAS(14, Material.AMETHYST_SHARD, "<gradient:#b388ff:#ff80ab>", "Tamias", "Tamias", "titan.navigator.tamias"), SURVIVAL(20, Material.GRASS_BLOCK, "<gradient:#7CFC00:#00C853>", "Survival", "Survival", null), CREATIVE(22, Material.WOODEN_AXE, "<gradient:#ff6ec7:#7873f5>", "Creative", "MemberBuild", null), BUILD(24, Material.SCAFFOLDING, "<gradient:#FFD700:#FF8C00>", "Build", "Team-Build", "titan.navigator.buildserver"),
    // Each staging entry has its own permission. The Slender one shares the proxy's join permission.
    SLENDER_STAGING(0, Material.STRUCTURE_BLOCK, "<gradient:#616161:#ffaa00>", "Slender (Staging)", "Slender-Staging", "cygnus.staging.join", NavigatorMessages.STAGING_HINT, true), BOUNCE_STAGING(1, Material.SLIME_BLOCK, "<gradient:#616161:#00FA9A>", "Bounce (Staging)", "Bounce-Staging", "bounce.staging.join", NavigatorMessages.STAGING_HINT, true), ELYTRA_RACE_STAGING(2, Material.FIREWORK_ROCKET, "<gradient:#616161:#fcba03>", "ElytraRace (Staging)", "ElytraRace-Staging", "elytrarace.staging.join", NavigatorMessages.STAGING_HINT, true), TAMIAS_STAGING(3, Material.AMETHYST_BLOCK, "<gradient:#616161:#b388ff>", "Tamias (Staging)", "Tamias-Staging", "tamias.staging.join", NavigatorMessages.STAGING_HINT, true);

    private final int slot;
    private final Material icon;
    private final String gradient;
    private final String label;
    private final String task;
    private final @Nullable String permission;
    private final @Nullable String hint;
    private final boolean staging;
    private final boolean onDemand;

    Destination(int slot, Material icon, String gradient, String label, String task, @Nullable String permission) {
        this(slot, icon, gradient, label, task, permission, null, false);
    }

    /**
     * {@code hint} is the translation key of one extra lore line, or {@code null} for none.
     * {@code staging} marks a staging environment, which is also started on demand and stopped when
     * idle.
     */
    Destination(int slot, Material icon, String gradient, String label, String task, @Nullable String permission, @Nullable String hint, boolean staging) {
        this.slot = slot;
        this.icon = icon;
        this.gradient = gradient;
        this.label = label;
        this.task = task;
        this.permission = permission;
        this.hint = hint;
        this.staging = staging;
        this.onDemand = staging;
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

    /** The translation key of the extra lore line, or {@code null} when the entry has none. */
    @Nullable
    String hint() {
        return this.hint;
    }

    /**
     * Whether the entry is a staging environment, which lives in the submenu and never on the main
     * grid.
     */
    boolean staging() {
        return this.staging;
    }

    /**
     * Whether the entry is started when a player asks for it and stopped once it stands idle. Only
     * staging entries are.
     */
    boolean onDemand() {
        return this.onDemand;
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

    /** The live entries with a permission: the ones the main grid gates. */
    static List<Destination> livePermissioned() {
        return Arrays.stream(values()).filter(destination -> destination.permission != null && !destination.staging).toList();
    }

    /** The staging entries, each behind its own permission. */
    static List<Destination> stagingEntries() {
        return Arrays.stream(values()).filter(destination -> destination.staging).toList();
    }

    // granted holds the permissioned destinations a menu shows; the others are always shown,
    // except staging entries, which never appear on the main grid.
    static List<Destination> visible(Set<Destination> granted) {
        return Arrays.stream(values()).filter(destination -> !destination.staging && (destination.permission == null || granted.contains(destination))).toList();
    }

    /** The staging entries the grants allow; the submenu shows them on their own slots. */
    static List<Destination> stagingVisible(Set<Destination> granted) {
        return Arrays.stream(values()).filter(destination -> destination.staging && granted.contains(destination)).toList();
    }
}
