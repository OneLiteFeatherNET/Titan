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
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

/**
 * One destination shown in the shared navigator inventories, fixed in code as a Java {@code enum}
 * since destinations change rarely. The live entries sit on the main grid, placed by their
 * {@link Group}. The staging entries sit in the first row of their submenu, slots 0 to 3, and never
 * on the main grid. Every entry but {@link #ELYTRA_RACE}, {@link #SURVIVAL} and {@link #CREATIVE}
 * is
 * gated behind a permission.
 */
enum Destination {

    ELYTRA_RACE(Group.MINIGAME, Material.ELYTRA, "<gradient:#fcba03:#03fc8c>", "ElytraRace", "ElytraRace", null, NavigatorMessages.DESCRIPTION_ELYTRA_RACE), BOUNCE(Group.MINIGAME, Material.SLIME_BALL, "<gradient:#00FA9A:#40E0D0>", "Bounce", "Bounce", "titan.navigator.bounce", NavigatorMessages.DESCRIPTION_BOUNCE), TAMIAS(Group.MINIGAME, Material.AMETHYST_SHARD, "<gradient:#b388ff:#ff80ab>", "Tamias", "Tamias", "titan.navigator.tamias", NavigatorMessages.DESCRIPTION_TAMIAS), SLENDER(Group.MINIGAME, Material.ENDERMAN_SPAWN_EGG, "<gradient:#616161:#e80000>", "Slender", "Slender", "titan.navigator.slender", NavigatorMessages.DESCRIPTION_SLENDER), SURVIVAL(Group.SERVER, Material.GRASS_BLOCK, "<gradient:#7CFC00:#00C853>", "Survival", "Survival", null, NavigatorMessages.DESCRIPTION_SURVIVAL), CREATIVE(Group.SERVER, Material.WOODEN_AXE, "<gradient:#ff6ec7:#7873f5>", "Creative", "MemberBuild", null, NavigatorMessages.DESCRIPTION_CREATIVE), BUILD(Group.SERVER, Material.SCAFFOLDING, "<gradient:#FFD700:#FF8C00>", "Build", "Team-Build", "titan.navigator.buildserver", NavigatorMessages.DESCRIPTION_BUILD),
    // Each staging entry has its own permission. The Slender one shares the proxy's join permission.
    SLENDER_STAGING(0, Material.STRUCTURE_BLOCK, "<gradient:#616161:#ffaa00>", "Slender (Staging)", "Slender-Staging", "cygnus.staging.join", NavigatorMessages.STAGING_HINT), BOUNCE_STAGING(1, Material.SLIME_BLOCK, "<gradient:#616161:#00FA9A>", "Bounce (Staging)", "Bounce-Staging", "bounce.staging.join", NavigatorMessages.STAGING_HINT), ELYTRA_RACE_STAGING(2, Material.FIREWORK_ROCKET, "<gradient:#616161:#fcba03>", "ElytraRace (Staging)", "ElytraRace-Staging", "elytrarace.staging.join", NavigatorMessages.STAGING_HINT), TAMIAS_STAGING(3, Material.AMETHYST_BLOCK, "<gradient:#616161:#b388ff>", "Tamias (Staging)", "Tamias-Staging", "tamias.staging.join", NavigatorMessages.STAGING_HINT);

    /** Where an entry sits: minigames in row one, servers in row two, staging in the submenu. */
    enum Group {
        MINIGAME, SERVER, STAGING
    }

    private final Group group;
    private final int slot;
    private final Material icon;
    private final String gradient;
    private final String label;
    private final String task;
    private final @Nullable String permission;
    private final @Nullable String description;
    private final @Nullable String hint;

    Destination(Group group, Material icon, String gradient, String label, String task, @Nullable String permission, String description) {
        this(group, -1, icon, gradient, label, task, permission, description, null);
    }

    /**
     * {@code hint} is the translation key of one extra lore line, or {@code null} for none. A
     * staging
     * entry is the only kind with a submenu {@code slot}.
     */
    Destination(int slot, Material icon, String gradient, String label, String task, @Nullable String permission, String hint) {
        this(Group.STAGING, slot, icon, gradient, label, task, permission, null, hint);
    }

    private Destination(Group group, int slot, Material icon, String gradient, String label, String task, @Nullable String permission, @Nullable String description, @Nullable String hint) {
        this.group = group;
        this.slot = slot;
        this.icon = icon;
        this.gradient = gradient;
        this.label = label;
        this.task = task;
        this.permission = permission;
        this.description = description;
        this.hint = hint;
    }

    Group group() {
        return this.group;
    }

    /**
     * The submenu slot of a staging entry; a main-grid entry has none, its slot comes from the
     * layout.
     */
    int slot() {
        return this.slot;
    }

    /** The name shown on the entry, which is not translated. */
    String label() {
        return this.label;
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
     * The translation key of the description line of a main-grid entry, or {@code null} for
     * staging.
     */
    @Nullable
    String description() {
        return this.description;
    }

    /** The translation key of the extra lore line, or {@code null} when the entry has none. */
    @Nullable
    String hint() {
        return this.hint;
    }

    /**
     * Whether the entry is a staging environment, which lives in the submenu and never on the main
     * grid. Such an entry is also started when a player asks for it and stopped once it stands
     * idle.
     */
    boolean staging() {
        return this.group == Group.STAGING;
    }

    /**
     * Whether the entry is started when a player asks for it and stopped once it stands idle. Only
     * staging entries are.
     */
    boolean onDemand() {
        return staging();
    }

    /**
     * The entry with its gradient name and the given lore. Rebuilt on every call, so no two callers
     * share a stack.
     */
    ItemStack item(List<Component> lore) {
        return ItemStack.builder(this.icon).customName(styledName()).lore(lore).build();
    }

    /**
     * A main-grid entry: the stack amount is its player count, and it glints while anyone is on it.
     */
    ItemStack item(List<Component> lore, int online) {
        ItemStack.Builder builder = ItemStack.builder(this.icon).customName(styledName()).lore(lore).amount(NavigatorItems.amountFor(online));
        if (NavigatorItems.glintsFor(online)) {
            builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return builder.build();
    }

    private Component styledName() {
        return MiniMessage.miniMessage().deserialize("<!i>" + this.gradient + this.label + "</gradient>");
    }

    static List<Destination> permissioned() {
        return Arrays.stream(values()).filter(destination -> destination.permission != null).toList();
    }

    /** The live entries with a permission: the ones the main grid gates. */
    static List<Destination> livePermissioned() {
        return Arrays.stream(values()).filter(destination -> destination.permission != null && !destination.staging()).toList();
    }

    /** The staging entries, each behind its own permission. */
    static List<Destination> stagingEntries() {
        return Arrays.stream(values()).filter(Destination::staging).toList();
    }

    // granted holds the permissioned destinations a menu shows; the others are always shown,
    // except staging entries, which never appear on the main grid.
    static List<Destination> visible(Set<Destination> granted) {
        return Arrays.stream(values()).filter(destination -> !destination.staging() && (destination.permission == null || granted.contains(destination))).toList();
    }

    /** The staging entries the grants allow; the submenu shows them on their own slots. */
    static List<Destination> stagingVisible(Set<Destination> granted) {
        return Arrays.stream(values()).filter(destination -> destination.staging() && granted.contains(destination)).toList();
    }
}
