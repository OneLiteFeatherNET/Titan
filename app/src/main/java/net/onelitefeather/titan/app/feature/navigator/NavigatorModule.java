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

import io.avaje.config.Config;
import io.avaje.inject.Priority;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.common.config.ConfigValues;
import net.onelitefeather.titan.common.feature.FeatureFlags;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one Aves-built inventory shared by
 * every player, listing every destination contributed to {@link NavigatorEntries} - this module's
 * own configured destinations and any other module's.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/design.md}, decision 8, and the
 * {@code lobby-navigator} spec: inventories run through Aves project-wide. The shared inventory
 * ({@link NavigatorInventory}) is built by one Aves {@code GlobalInventoryBuilder}, rebuilt only
 * when {@link NavigatorEntries#version()} changes, with {@link NavigatorInventory#register()}
 * called exactly once here in {@link #enable} and {@link NavigatorInventory#unregister()} exactly
 * once in {@link #disable}. This module registers no {@code InventoryPreClickEvent} listener of its
 * own - every entry slot carries its own Aves click handler that cancels the click, forwards
 * through {@link Deliver} and closes the inventory; see {@link NavigatorInventory}'s Javadoc for
 * why
 * that also makes this module's click handling independent of whether
 * {@code feature.protection.ProtectionModule} is enabled before or after it.
 *
 * <p>{@link #entries} is handed in through the constructor rather than read from {@code context},
 * because {@link ModuleContext#navigator()} only exposes the narrow, add-only
 * {@link NavigatorEntries.View} - this module needs to read back every module's entries at open
 * time, not just add its own.
 *
 * <p>{@link #featureFlags} gates entries behind a feature flag (see {@code design.md}, decision
 * 13):
 * injected via the constructor rather than read from the static Togglz {@code FeatureContext}
 * directly, so a test can hand in a fake instead of a real {@code flags.properties} file. This
 * module hands the very same instance to its constructor, and hands the platform-wide entry
 * registry to its own {@code enable}; the composition root ({@code Titan}) wires that same
 * {@link FeatureFlags} instance into {@code ModuleRegistry.Builder#featureFlags} too, so every
 * configured entry's {@link NavigatorEntry#feature()} - and every other module's entries'
 * {@code feature()} besides - is validated up front, once every module has enabled, by
 * {@link NavigatorEntries#validate(FeatureFlags)}, not by this module itself.
 *
 * <p>{@link #enable} reads its title and entries directly from the {@code io.avaje.config.Config}
 * static facade - the title via {@code Config.get("navigator.title")}, the entries by turning the
 * keys {@code Config.asConfiguration().forPath("navigator.entries").keys()} returns into names via
 * {@link NavigatorEntryKeys#names(Set)}, validating each one's values with
 * {@link NavigatorEntryValidation#buildEntry} and deserializing the result into a renderable
 * {@link NavigatorEntry} - per {@code openspec/changes/avaje-config-facade/design.md}, decision 6.
 * No default values are supplied in code; every key is expected to exist in the shipped classpath
 * {@code application.yaml}.
 */
@Singleton
@Priority(400)
public final class NavigatorModule implements LobbyModule {

    private static final String ID = "navigator";
    private static final Key ITEM_KEY = Key.key("titan:navigator");
    private static final int HOTBAR_SLOT = 4;
    private static final String TITLE_KEY = "navigator.title";
    private static final String ENTRIES_PATH = "navigator.entries";

    private final Deliver deliver;
    private final NavigatorEntries entries;
    private final FeatureFlags featureFlags;
    private NavigatorInventory navigatorInventory;

    /**
     * @param deliver      the delivery service a navigator click forwards the player through
     * @param entries      the platform-wide navigator entry registry this module renders
     * @param featureFlags the source of truth an entry's optional feature gate is checked against
     */
    public NavigatorModule(Deliver deliver, NavigatorEntries entries, FeatureFlags featureFlags) {
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.entries = Objects.requireNonNull(entries, "entries must not be null");
        this.featureFlags = Objects.requireNonNull(featureFlags, "featureFlags must not be null");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void enable(ModuleContext context) {
        Component title = MiniMessage.miniMessage().deserialize(Config.get(TITLE_KEY));
        this.navigatorInventory = new NavigatorInventory(title, this.entries, this.featureFlags, this::onSelect);

        for (NavigatorEntry entry : readEntries()) {
            context.navigator().add(entry);
        }

        ItemStack feather = ItemStack.builder(Material.FEATHER).customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")).build();
        context.items().register(new LobbyItem(ITEM_KEY, feather, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> player.openInventory(this.navigatorInventory.current())));

        this.navigatorInventory.register();
    }

    /**
     * Reads every entry under {@code navigator.entries}: the entry names come from
     * {@link #readEntryNames()}, each name's own {@code slot}, {@code icon}, {@code displayName},
     * {@code destination} and optional {@code feature} are read and validated by
     * {@link NavigatorEntryValidation#buildEntry}, and the result is turned into a renderable
     * {@link NavigatorEntry} by {@link #toNavigatorEntry}.
     *
     * @return every configured navigator entry, ready to register
     */
    private static List<NavigatorEntry> readEntries() {
        Set<String> names = readEntryNames();
        List<NavigatorEntry> entries = new ArrayList<>();
        for (String name : names) {
            String prefix = ENTRIES_PATH + "." + name + ".";
            int slot = ConfigValues.intValue(prefix + "slot");
            String icon = Config.get(prefix + "icon");
            String displayName = Config.get(prefix + "displayName");
            String destination = Config.get(prefix + "destination");
            String feature = Config.getNullable(prefix + "feature");
            NavigatorEntryValidation.ConfiguredNavigatorEntry configured = NavigatorEntryValidation.buildEntry(name, slot, icon, displayName, destination, feature);
            entries.add(toNavigatorEntry(configured));
        }
        return entries;
    }

    /**
     * Reads every configured {@code navigator.entries} entry name:
     * {@link NavigatorEntryKeys#names(Set)} applied to
     * {@code Config.asConfiguration().forPath(ENTRIES_PATH).keys()}.
     *
     * <p>Package-private, rather than inlined into {@link #readEntries()}, purely so
     * {@code net.onelitefeather.titan.app.feature.navigator.NavigatorValidation} (a test helper in
     * the same package) can reuse it instead of repeating this read line - see
     * {@code openspec/changes/avaje-config-facade/design.md}, decision 3.
     *
     * @return every configured {@code navigator.entries} entry name
     */
    static Set<String> readEntryNames() {
        return NavigatorEntryKeys.names(Config.asConfiguration().forPath(ENTRIES_PATH).keys());
    }

    private static NavigatorEntry toNavigatorEntry(NavigatorEntryValidation.ConfiguredNavigatorEntry entry) {
        Component displayName = MiniMessage.miniMessage().deserialize(entry.displayName());
        ItemStack icon = ItemStack.builder(Material.fromKey(entry.icon())).customName(displayName).build();
        return new NavigatorEntry(entry.slot(), icon, displayName, entry.destination(), entry.feature());
    }

    @Override
    public void disable() {
        this.navigatorInventory.unregister();
    }

    /**
     * Test-only: the shared Aves inventory this module opens for every player, so a leak test can
     * assert the listener count on the event node Aves registered on (not just this module's own
     * node) stays constant across opens.
     *
     * @return the shared inventory
     */
    Inventory sharedInventory() {
        return this.navigatorInventory.current();
    }

    private void onSelect(Player player, NavigatorEntry entry) {
        this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(entry.destination()).player(player).build());
    }
}
