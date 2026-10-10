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
package net.onelitefeather.titan.feature.lobbyswitcher;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.avaje.inject.Profile;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.theevilreaper.aves.i18n.TextData;
import net.theevilreaper.aves.inventory.GlobalTranslatedInventoryBuilder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.item.TranslatedItem;
import org.jetbrains.annotations.Nullable;

/**
 * The lobby list: one Aves {@link GlobalTranslatedInventoryBuilder} for the whole module, rendered
 * per viewer locale. The content is the last reading, taken when a player opens it.
 *
 * <p>The builder's size is fixed, so the list always has six rows, the most {@link SwitcherLayout}
 * allows; slots without an entry hold a filler because Aves does not clear a slot that becomes
 * empty.
 */
@Singleton
@Profile(LobbySwitcherModule.CLOUDNET)
final class SwitcherInventory {

    private static final InventoryType TYPE = InventoryType.CHEST_6_ROW;
    private static final ItemStack FILLER = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private final GlobalTranslatedInventoryBuilder builder = new EnglishWithoutLocale();
    private final SwitcherReading reading;
    private volatile SwitcherReading.Outcome outcome = new SwitcherReading.Unavailable();
    private volatile LobbyIdentity own;
    private boolean registered;

    SwitcherInventory(LobbySwitcherMessages messages, PlayerCounts counts) {
        Objects.requireNonNull(messages, "messages must not be null");
        this.reading = new SwitcherReading(counts);
        this.builder.setTitleData(new TextData(LobbySwitcherMessages.TITLE));
        this.builder.setLayout(InventoryLayout.fromType(TYPE));
        this.builder.setDataLayoutFunction(previous -> layout());
    }

    @PostConstruct
    void start() {
        this.builder.register();
        this.registered = true;
    }

    /** Idempotent. Closes the inventory for everyone who has it open. */
    @PreDestroy
    void stop() {
        if (this.registered) {
            this.registered = false;
            this.builder.unregister();
        }
    }

    /** Reads the services of {@code own}'s task now and shows them to {@code player}. */
    void open(Player player, LobbyIdentity own) {
        this.own = own;
        this.outcome = this.reading.read(own.task());
        this.builder.invalidateDataLayout();
        Locale locale = Objects.requireNonNullElse(player.getLocale(), Locale.ENGLISH);
        player.openInventory(this.builder.getInventory(locale));
    }

    private InventoryLayout layout() {
        InventoryLayout layout = InventoryLayout.fromType(TYPE);
        for (int slot = 0; slot < layout.getSize(); slot++) {
            layout.setItem(slot, FILLER);
        }
        switch (this.outcome) {
            case SwitcherReading.Fresh fresh -> place(layout, fresh.services());
            case SwitcherReading.Stale stale -> place(layout, stale.services());
            case SwitcherReading.Unavailable ignored ->
                layout.setItem(0, TranslatedItem.of(Material.BARRIER).setDisplayName(new TextData(LobbySwitcherMessages.STATE_UNAVAILABLE)).toNonClickSlot());
        }
        return layout;
    }

    private void place(InventoryLayout layout, List<ServiceCount> services) {
        List<SwitcherEntry> entries = SwitcherLayout.fit(SwitcherEntry.sorted(services, this.own));
        for (int slot = 0; slot < entries.size(); slot++) {
            layout.setItem(slot, item(entries.get(slot)).toNonClickSlot());
        }
    }

    private static TranslatedItem item(SwitcherEntry entry) {
        String online = String.valueOf(entry.online());
        String max = String.valueOf(entry.max());
        return switch (entry.state()) {
            case CURRENT ->
                row(Material.NETHER_STAR, entry, new TextData(LobbySwitcherMessages.STATE_CURRENT, online, max));
            case FULL ->
                row(Material.RED_CONCRETE, entry, new TextData(LobbySwitcherMessages.STATE_FULL, online, max));
            case NOT_READY ->
                row(Material.YELLOW_CONCRETE, entry, new TextData(LobbySwitcherMessages.STATE_NOT_READY));
            case JOINABLE ->
                row(Material.LIME_CONCRETE, entry, new TextData(LobbySwitcherMessages.ENTRY_COUNT, online, max));
        };
    }

    private static TranslatedItem row(Material material, SwitcherEntry entry, TextData lore) {
        return TranslatedItem.of(material).setDisplayName(new TextData(LobbySwitcherMessages.ENTRY_NAME, entry.name())).setLore(lore);
    }

    /**
     * Aves asks for the inventory without a locale when it registers its listeners, and then fails
     * to render the title; English is the fallback bundle anyway.
     */
    private static final class EnglishWithoutLocale extends GlobalTranslatedInventoryBuilder {

        EnglishWithoutLocale() {
            super(TYPE);
        }

        @Override
        public Inventory getInventory(@Nullable Locale locale) {
            return super.getInventory(locale == null ? Locale.ENGLISH : locale);
        }
    }
}
