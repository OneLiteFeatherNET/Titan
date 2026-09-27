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
import io.avaje.config.Configuration;
import io.avaje.inject.Priority;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
 * injected via the constructor rather than read through the static {@code io.avaje.config.Config}
 * facade directly, so a test can hand in a fake instead of a real {@code application.yaml} file.
 * This
 * module hands the very same instance to its constructor, and hands the platform-wide entry
 * registry to its own {@code enable}; the composition root ({@code Titan}) wires that same
 * {@link FeatureFlags} instance into {@code ModuleRegistry.Builder#featureFlags} too, so every
 * configured entry's {@link NavigatorEntry#feature()} - and every other module's entries'
 * {@code feature()} besides - is validated up front, once every module has enabled, by
 * {@link NavigatorEntries#validate(FeatureFlags)}, not by this module itself.
 *
 * <p>{@link #enable} reads its title and entries once, directly from the
 * {@code io.avaje.config.Config} static facade - the title via
 * {@code Config.get("navigator.title")},
 * the entries by turning the keys
 * {@code Config.asConfiguration().forPath("navigator.entries").keys()} returns into names via
 * {@link NavigatorEntryKeys#names(Set)}, validating each one's values with
 * {@link NavigatorEntryValidation#buildEntry} and deserializing the result into a renderable
 * {@link NavigatorEntry} - per {@code openspec/changes/avaje-config-facade/design.md}, decision 6.
 * That first read is strict: an unknown feature name or an invalid entry still aborts startup
 * (unchanged behaviour), since {@code ModuleRegistry#enableAll()} runs
 * {@link NavigatorEntries#validate(FeatureFlags)} once every module has enabled.
 *
 * <p>Every later open re-reads title and entries the same way (see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decisions 1 and 2): the
 * {@code titan:navigator} item's use handler calls {@link #refreshFromConfig()} before opening the
 * inventory, which reads {@link #currentTitle()} - applied via
 * {@link NavigatorInventory#applyTitleIfChanged(Component)} - and {@link #currentEntries()},
 * which it re-attributes to this module's own id in {@link #entries} (replacing this module's
 * previous contribution, leaving any other module's entries untouched). Configuration is
 * validated only once, at startup: unlike the one strict read in {@link #enable} - which still
 * aborts the whole start on an unknown feature name or an invalid entry - a later open never
 * re-validates or falls back to a shipped default (see {@code refactor/drop-runtime-fallback}).
 * An unknown feature name or an invalid entry found on a later open therefore fails that one open
 * instead: {@link #currentEntries()} throws out of the item's use handler, so the inventory does
 * not open for that click, until an operator corrects the value.
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
        // Strict, one-time read: an unknown feature name or an invalid entry here still aborts
        // startup, via NavigatorEntries#validate(FeatureFlags) in ModuleRegistry#enableAll()
        // (unchanged behaviour).
        Component title = readTitle(Config.asConfiguration());
        this.navigatorInventory = new NavigatorInventory(title, this.entries, this.featureFlags, this::onSelect);

        for (NavigatorEntry entry : readEntries(Config.asConfiguration())) {
            context.navigator().add(entry);
        }

        ItemStack feather = ItemStack.builder(Material.FEATHER).customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")).build();
        context.items().register(new LobbyItem(ITEM_KEY, feather, ItemSlot.hotbar(HOTBAR_SLOT), (player, event) -> {
            refreshFromConfig();
            player.openInventory(this.navigatorInventory.current());
        }));

        this.navigatorInventory.register();
    }

    /**
     * Re-reads title and entries from the live configuration and applies them - called on every
     * open, before {@link NavigatorInventory#current()} (see {@code openspec/changes/
     * config-reload-feature-flags/design.md}, decisions 1 and 2): a changed
     * {@code navigator.title} or {@code navigator.entries} applies to the very next open, without
     * a module restart. Unlike {@link #enable}'s one strict read, an invalid value found here is
     * never re-validated against a fallback - it simply fails this one open (see
     * {@code refactor/drop-runtime-fallback}) - see {@link #currentTitle()} and
     * {@link #currentEntries()}.
     */
    private void refreshFromConfig() {
        this.navigatorInventory.applyTitleIfChanged(currentTitle());

        this.entries.removeAll(ID);
        for (NavigatorEntry entry : currentEntries()) {
            this.entries.add(ID, entry);
        }
    }

    /**
     * @return the current {@code navigator.title}, read live through the facade
     */
    private static Component currentTitle() {
        return readTitle(Config.asConfiguration());
    }

    /**
     * @return the current, valid navigator entries, resolved via {@link #resolveEntries}
     */
    private List<NavigatorEntry> currentEntries() {
        return resolveEntries(Config.asConfiguration(), this.featureFlags);
    }

    /**
     * The runtime counterpart of {@link #enable}'s strict entry read: reads every entry under
     * {@code navigator.entries} from {@code live} as a raw, not-yet-parsed snapshot (see
     * {@link #rawEntrySection(Configuration)}), then builds and validates it, additionally
     * checking every entry's optional feature name against {@code featureFlags} (unlike
     * {@link #readEntries(Configuration)} alone, which knows nothing about feature flags at all).
     * Configuration is validated only once, at startup (see {@code refactor/drop-runtime-fallback}
     * ): if that fails for any reason - an unknown feature name, an out-of-range slot, an unknown
     * material, a blank destination - it throws, failing this one open, instead of falling back to
     * a shipped default.
     *
     * <p>Takes {@code live} as a plain parameter - never touching the static
     * {@code io.avaje.config.Config} facade itself - so a test can build a {@link Configuration}
     * instance directly (e.g. {@code Configuration.builder().put(key, value).build()}) instead of
     * relying on a real classpath resource.
     *
     * @param live         the live configuration to read {@code navigator.entries} from
     * @param featureFlags the source of truth an entry's optional feature gate is checked against
     * @return the resolved navigator entries
     * @throws RuntimeException if any entry is invalid, or names an unknown feature
     */
    static List<NavigatorEntry> resolveEntries(Configuration live, FeatureFlags featureFlags) {
        return buildKnownEntries(rawEntrySection(live), featureFlags);
    }

    /**
     * @param raw          one {@code navigator.entries} section's raw, not-yet-parsed values (see
     *                     {@link #rawEntrySection(Configuration)})
     * @param featureFlags the source of truth an entry's optional feature gate is checked against
     * @return every entry {@code raw} describes, built and validated, including the feature check
     * @throws RuntimeException if any entry is invalid, or names an unknown feature
     */
    private static List<NavigatorEntry> buildKnownEntries(SortedMap<String, String> raw, FeatureFlags featureFlags) {
        List<NavigatorEntry> entries = buildEntries(raw);
        requireKnownFeatures(entries, featureFlags);
        return entries;
    }

    /**
     * @param entries      the entries to check
     * @param featureFlags the source of truth to check each entry's optional feature name against
     * @throws IllegalArgumentException if any entry names a feature {@code featureFlags} does not
     *                                  recognize
     */
    private static void requireKnownFeatures(List<NavigatorEntry> entries, FeatureFlags featureFlags) {
        for (NavigatorEntry entry : entries) {
            String feature = entry.feature();
            if (feature != null && !featureFlags.exists(feature)) {
                throw new IllegalArgumentException(ENTRIES_PATH + ": entry '" + entry.destination() + "' uses unknown feature flag '" + feature + "'");
            }
        }
    }

    /**
     * @param configuration the source to read {@link #TITLE_KEY} from - the live facade
     *                      ({@link #enable}, {@link #currentTitle()}) or the shipped classpath
     *                      defaults (never needed today: {@code navigator.title} has no
     *                      validation that can fail, but kept symmetric with
     *                      {@link #readEntries(Configuration)})
     * @return the deserialized title
     */
    private static Component readTitle(Configuration configuration) {
        return MiniMessage.miniMessage().deserialize(configuration.get(TITLE_KEY));
    }

    /**
     * Reads every entry under {@code navigator.entries} from {@code configuration}, built and
     * validated by {@link #buildEntries(SortedMap)} - see that method's Javadoc for the
     * reading/building split. Takes the source as a parameter - the live facade or the shipped
     * classpath defaults - rather than always reading the static facade, so {@link #resolveEntries}
     * can reuse this exact logic for the shipped fallback (see design.md, decision 2: DRY, one
     * reading/validating path).
     *
     * @param configuration the source to read {@code navigator.entries} from
     * @return every entry {@code configuration} describes, ready to register
     */
    private static List<NavigatorEntry> readEntries(Configuration configuration) {
        return buildEntries(rawEntrySection(configuration));
    }

    /**
     * Reads {@code navigator.entries} from {@code configuration} as a raw, not-yet-parsed
     * snapshot: every relative key {@code configuration.forPath(ENTRIES_PATH).keys()} returns
     * (e.g. {@code survival.slot}, {@code survival.icon}) mapped to its plain string value. A
     * {@link SortedMap} rather than {@link Configuration} itself, so {@link #buildEntries} can
     * read it without any {@code io.avaje.config.Config}/{@link Configuration} type of its own.
     *
     * @param configuration the source to read {@code navigator.entries} from
     * @return every relative key under {@code navigator.entries}, mapped to its raw value
     */
    private static SortedMap<String, String> rawEntrySection(Configuration configuration) {
        Set<String> relativeKeys = configuration.forPath(ENTRIES_PATH).keys();
        SortedMap<String, String> raw = new TreeMap<>();
        for (String relativeKey : relativeKeys) {
            raw.put(relativeKey, configuration.getNullable(ENTRIES_PATH + "." + relativeKey));
        }
        return raw;
    }

    /**
     * Builds and validates every entry {@code raw} describes: the entry names come from
     * {@link NavigatorEntryKeys#names(Set)} applied to {@code raw}'s keys, each name's own
     * {@code slot}, {@code icon}, {@code displayName}, {@code destination} and optional
     * {@code feature} are read out of {@code raw} and validated by
     * {@link NavigatorEntryValidation#buildEntry}, and the result is turned into a renderable
     * {@link NavigatorEntry} by {@link #toNavigatorEntry}. Deliberately free of any
     * {@code io.avaje.config.Config}/{@link Configuration} type - unlike {@link #readEntries},
     * this does not know whether {@code raw} came from the live facade or the shipped classpath
     * defaults - so {@link #buildKnownEntries} can reuse it for the live, unvalidated re-read too.
     *
     * @param raw one {@code navigator.entries} section's raw, not-yet-parsed values (see
     *            {@link #rawEntrySection(Configuration)})
     * @return every entry {@code raw} describes, ready to register
     * @throws RuntimeException if any entry is invalid
     */
    private static List<NavigatorEntry> buildEntries(SortedMap<String, String> raw) {
        Set<String> names = NavigatorEntryKeys.names(raw.keySet());
        List<NavigatorEntry> entries = new ArrayList<>();
        for (String name : names) {
            String prefix = name + ".";
            int slot = Integer.parseInt(raw.get(prefix + "slot"));
            String icon = raw.get(prefix + "icon");
            String displayName = raw.get(prefix + "displayName");
            String destination = raw.get(prefix + "destination");
            String feature = raw.get(prefix + "feature");
            NavigatorEntryValidation.ConfiguredNavigatorEntry configured = NavigatorEntryValidation.buildEntry(name, slot, icon, displayName, destination, feature);
            entries.add(toNavigatorEntry(configured));
        }
        return entries;
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
