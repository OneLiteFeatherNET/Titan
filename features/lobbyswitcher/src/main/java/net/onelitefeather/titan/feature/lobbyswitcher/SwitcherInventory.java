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
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.theevilreaper.aves.i18n.TextData;
import net.theevilreaper.aves.inventory.CustomInventory;
import net.theevilreaper.aves.inventory.GlobalTranslatedInventoryBuilder;
import net.theevilreaper.aves.inventory.holder.InventoryHolderImpl;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.item.TranslatedItem;
import org.jetbrains.annotations.Nullable;

/**
 * The lobby list: one Aves {@link GlobalTranslatedInventoryBuilder} for the whole module, rendered
 * per viewer locale. Opening reads the services off the tick and shows the list on the next tick;
 * while somebody has it open the content is read again every refresh period.
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
    private final Scheduler scheduler;
    private final ViewerCounter viewers;
    private final SwitcherSelection selection;
    private final LobbySwitcherTelemetry telemetry;
    // Players waiting for the read that shows them the list.
    private final Queue<Player> opening = new ConcurrentLinkedQueue<>();
    private volatile SwitcherReading.Outcome outcome = new SwitcherReading.Unavailable();
    private volatile LobbyIdentity own;
    private volatile boolean stopped;
    private boolean registered;

    SwitcherInventory(LobbySwitcherMessages messages, PlayerCounts counts, Scheduler scheduler, @Named(LobbySwitcherBeans.READS) Executor reads, Deliver deliver, Telemetry telemetry, LobbySwitcherSettings settings) {
        Objects.requireNonNull(messages, "messages must not be null");
        this.reading = new SwitcherReading(counts);
        this.scheduler = scheduler;
        this.telemetry = new LobbySwitcherTelemetry(telemetry);
        SchedulerReadScheduling scheduling = new SchedulerReadScheduling(scheduler, reads, () -> this.reading.read(this.own.task()), this::apply);
        this.viewers = new ViewerCounter(scheduling, settings.refreshSeconds());
        this.selection = new SwitcherSelection(counts, scheduler, reads, deliver, messages, this.telemetry, scheduling::requestRead);
        this.builder.setTitleData(new TextData(LobbySwitcherMessages.TITLE));
        this.builder.setLayout(InventoryLayout.fromType(TYPE));
        this.builder.setDataLayoutFunction(previous -> layout());
    }

    @PostConstruct
    void start() {
        this.builder.register();
        this.registered = true;
    }

    /** Idempotent. Closes the inventory for everyone who has it open and ends the period. */
    @PreDestroy
    void stop() {
        this.stopped = true;
        this.opening.clear();
        this.viewers.stop();
        if (this.registered) {
            this.registered = false;
            this.builder.unregister();
        }
    }

    /**
     * Reads the services of {@code own}'s task off the tick and shows them to {@code player} on the
     * next tick, so the list never opens with a stale or empty reading.
     */
    void open(Player player, LobbyIdentity own) {
        if (this.stopped) {
            return;
        }
        this.own = own;
        this.opening.add(player);
        this.viewers.opened();
    }

    /**
     * Cancels every click in the list and treats a click on an entry as a selection.
     *
     * <p>Aves' own click and close listeners never fire for a translated builder (each locale's
     * inventory gets a new holder, and Aves compares holders by identity), so the module forwards
     * these two events here instead.
     */
    void onClick(InventoryPreClickEvent event) {
        if (!isOurs(event.getInventory())) {
            return;
        }
        event.setCancelled(true);
        List<SwitcherEntry> entries = entries();
        int slot = event.getSlot();
        if (this.own != null && slot >= 0 && slot < entries.size()) {
            this.selection.select(event.getPlayer(), this.own, entries.get(slot).name());
        }
    }

    /** A viewer left the list; the last one stops the period. */
    void onClose(InventoryCloseEvent event) {
        if (isOurs(event.getInventory())) {
            this.viewers.closed();
        }
    }

    private boolean isOurs(AbstractInventory inventory) {
        return inventory instanceof CustomInventory custom && custom.getHolder() instanceof InventoryHolderImpl holder && holder.inventoryBuilder() == this.builder;
    }

    // Tick thread: the only place the inventory content changes.
    private void apply(SwitcherReading.Outcome read) {
        if (this.stopped) {
            return;
        }
        this.outcome = read;
        this.builder.invalidateDataLayout();
        if (!this.opening.isEmpty()) {
            // Aves recomputes the content on the next tick, and only once an inventory asks for
            // it; asking now and opening on that tick shows the new content.
            this.opening.forEach(player -> this.builder.getInventory(localeOf(player)));
            this.scheduler.scheduleNextTick(this::showOpening);
        }
    }

    private static Locale localeOf(Player player) {
        return Objects.requireNonNullElse(player.getLocale(), Locale.ENGLISH);
    }

    private void showOpening() {
        for (Player player = this.opening.poll(); player != null; player = this.opening.poll()) {
            show(player);
        }
    }

    private void show(Player player) {
        if (!player.isOnline()) {
            this.viewers.closed();
            return;
        }
        this.telemetry.open(player.getUuid(), entries().size(), () -> player.openInventory(this.builder.getInventory(localeOf(player))));
    }

    private List<SwitcherEntry> entries() {
        return switch (this.outcome) {
            case SwitcherReading.Fresh fresh ->
                SwitcherLayout.fit(SwitcherEntry.sorted(fresh.services(), this.own));
            case SwitcherReading.Stale stale ->
                SwitcherLayout.fit(SwitcherEntry.sorted(stale.services(), this.own));
            case SwitcherReading.Unavailable ignored -> List.of();
        };
    }

    private InventoryLayout layout() {
        InventoryLayout layout = InventoryLayout.fromType(TYPE);
        for (int slot = 0; slot < layout.getSize(); slot++) {
            layout.setItem(slot, FILLER);
        }
        if (this.outcome instanceof SwitcherReading.Unavailable) {
            layout.setItem(0, TranslatedItem.of(Material.BARRIER).setDisplayName(new TextData(LobbySwitcherMessages.STATE_UNAVAILABLE)).toNonClickSlot());
            return layout;
        }
        List<SwitcherEntry> entries = entries();
        for (int slot = 0; slot < entries.size(); slot++) {
            layout.setItem(slot, item(entries.get(slot)).toNonClickSlot());
        }
        return layout;
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
