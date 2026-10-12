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
import java.util.stream.IntStream;
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
 * The lobby list: one Aves {@link GlobalTranslatedInventoryBuilder} per page, each rendered per
 * viewer locale. Opening reads the services off the tick and shows the first page on the next
 * tick; while somebody has it open the content is read again every refresh period.
 *
 * <p>A builder's size is fixed, so the builders are rebuilt whenever the lobbies need another
 * {@link SwitcherLayout}; viewers of the old ones are closed. A page is a builder of its own, so a
 * viewer's page is simply the inventory they have open and nothing is stored per player. Slots
 * without an entry hold a filler because Aves does not clear a slot that becomes empty.
 */
@Singleton
@Profile(LobbySwitcherModule.CLOUDNET)
final class SwitcherInventory {

    private static final List<InventoryType> TYPES = List.of(InventoryType.CHEST_1_ROW, InventoryType.CHEST_2_ROW, InventoryType.CHEST_3_ROW, InventoryType.CHEST_4_ROW, InventoryType.CHEST_5_ROW, InventoryType.CHEST_6_ROW);
    private static final ItemStack FILLER = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private final SwitcherReading reading;
    private final Scheduler scheduler;
    private final ViewerCounter viewers;
    private final SwitcherSelection selection;
    private final LobbySwitcherTelemetry telemetry;
    // Players waiting for the read that shows them the list.
    private final Queue<Player> opening = new ConcurrentLinkedQueue<>();
    private volatile SwitcherReading.Outcome outcome = new SwitcherReading.Unavailable();
    private volatile LobbyIdentity own;
    // The pages of the current layout; rebuilt on the tick thread only.
    private volatile SwitcherLayout layout = SwitcherLayout.of(0);
    private volatile List<GlobalTranslatedInventoryBuilder> pages = List.of();
    private Player turning;
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
    }

    @PostConstruct
    void start() {
        this.pages = build(this.layout);
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
            this.pages.forEach(GlobalTranslatedInventoryBuilder::unregister);
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
        int page = pageOf(event.getInventory());
        int slot = event.getSlot();
        if (this.layout.paged() && turn(event.getPlayer(), page, slot)) {
            return;
        }
        List<SwitcherEntry> entries = this.layout.page(entries(), page);
        if (this.own != null && slot >= 0 && slot < entries.size()) {
            this.selection.select(event.getPlayer(), this.own, entries.get(slot).name());
        }
    }

    // True when the slot is in the navigation row, whether or not its arrow is shown.
    private boolean turn(Player player, int page, int slot) {
        if (slot < SwitcherLayout.PAGE_ENTRIES) {
            return false;
        }
        int target = switch (slot) {
            case SwitcherLayout.PREVIOUS_SLOT -> page - 1;
            case SwitcherLayout.NEXT_SLOT -> page + 1;
            default -> -1;
        };
        if (target >= 0 && target < this.layout.pages()) {
            // Opening replaces the open inventory, which must not count as the viewer leaving.
            this.turning = player;
            player.openInventory(this.pages.get(target).getInventory(localeOf(player)));
            this.turning = null;
        }
        return true;
    }

    /** A viewer left the list; the last one stops the period. */
    void onClose(InventoryCloseEvent event) {
        if (isOurs(event.getInventory()) && event.getPlayer() != this.turning) {
            this.viewers.closed();
        }
    }

    /**
     * A player left with the list open; Minestom fires no close event for that, so the viewer is
     * released here.
     */
    void onDisconnect(Player player) {
        AbstractInventory open = player.getOpenInventory();
        if (open != null && isOurs(open)) {
            this.viewers.closed();
        }
    }

    private boolean isOurs(AbstractInventory inventory) {
        return pageOf(inventory) >= 0;
    }

    // The page the inventory shows, or -1 when it is none of ours.
    private int pageOf(AbstractInventory inventory) {
        if (inventory instanceof CustomInventory custom && custom.getHolder() instanceof InventoryHolderImpl holder) {
            return this.pages.indexOf(holder.inventoryBuilder());
        }
        return -1;
    }

    // Tick thread: the only place the inventory content changes.
    private void apply(SwitcherReading.Outcome read) {
        if (this.stopped) {
            return;
        }
        this.outcome = read;
        resize(SwitcherLayout.of(entries().size()));
        this.pages.forEach(GlobalTranslatedInventoryBuilder::invalidateDataLayout);
        if (!this.opening.isEmpty()) {
            // Aves recomputes the content on the next tick, and only once an inventory asks for
            // it; asking now and opening on that tick shows the new content.
            this.opening.forEach(player -> this.pages.getFirst().getInventory(localeOf(player)));
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
        this.telemetry.open(player.getUuid(), entries().size(), () -> player.openInventory(this.pages.getFirst().getInventory(localeOf(player))));
    }

    private List<SwitcherEntry> entries() {
        return switch (this.outcome) {
            case SwitcherReading.Fresh fresh ->
                SwitcherEntry.sorted(fresh.services(), this.own);
            case SwitcherReading.Stale stale ->
                SwitcherEntry.sorted(stale.services(), this.own);
            case SwitcherReading.Unavailable ignored -> List.of();
        };
    }

    // A new size cannot be applied to a builder, so the old pages go; their viewers are closed.
    private void resize(SwitcherLayout wanted) {
        if (wanted.equals(this.layout)) {
            return;
        }
        // Aves still recomputes a retired page; it must not lay out entries it has no room for.
        InventoryLayout empty = InventoryLayout.fromType(TYPES.get(this.layout.rows() - 1));
        this.pages.forEach(page -> page.setDataLayoutFunction(previous -> empty));
        this.pages.forEach(GlobalTranslatedInventoryBuilder::unregister);
        this.layout = wanted;
        this.pages = build(wanted);
    }

    private List<GlobalTranslatedInventoryBuilder> build(SwitcherLayout wanted) {
        InventoryType type = TYPES.get(wanted.rows() - 1);
        return IntStream.range(0, wanted.pages()).mapToObj(page -> {
            GlobalTranslatedInventoryBuilder builder = new EnglishWithoutLocale(type);
            builder.setTitleData(new TextData(LobbySwitcherMessages.TITLE));
            builder.setLayout(InventoryLayout.fromType(type));
            builder.setDataLayoutFunction(previous -> pageLayout(type, page));
            builder.register();
            return builder;
        }).toList();
    }

    private InventoryLayout pageLayout(InventoryType type, int page) {
        InventoryLayout layout = InventoryLayout.fromType(type);
        for (int slot = 0; slot < layout.getSize(); slot++) {
            layout.setItem(slot, FILLER);
        }
        if (this.outcome instanceof SwitcherReading.Unavailable) {
            layout.setItem(0, TranslatedItem.of(Material.BARRIER).setDisplayName(new TextData(LobbySwitcherMessages.STATE_UNAVAILABLE)).toNonClickSlot());
            return layout;
        }
        List<SwitcherEntry> entries = this.layout.page(entries(), page);
        for (int slot = 0; slot < entries.size(); slot++) {
            layout.setItem(slot, item(entries.get(slot)).toNonClickSlot());
        }
        if (this.layout.paged()) {
            navigation(layout, page);
        }
        return layout;
    }

    private void navigation(InventoryLayout layout, int page) {
        String current = String.valueOf(page + 1);
        String total = String.valueOf(this.layout.pages());
        layout.setItem(SwitcherLayout.INDICATOR_SLOT, TranslatedItem.of(Material.PAPER).setDisplayName(new TextData(LobbySwitcherMessages.PAGE_INDICATOR, current, total)).toNonClickSlot());
        if (page > 0) {
            layout.setItem(SwitcherLayout.PREVIOUS_SLOT, TranslatedItem.of(Material.ARROW).setDisplayName(new TextData(LobbySwitcherMessages.PAGE_PREVIOUS)).toNonClickSlot());
        }
        if (page < this.layout.pages() - 1) {
            layout.setItem(SwitcherLayout.NEXT_SLOT, TranslatedItem.of(Material.ARROW).setDisplayName(new TextData(LobbySwitcherMessages.PAGE_NEXT)).toNonClickSlot());
        }
    }

    private static TranslatedItem item(SwitcherEntry entry) {
        String online = String.valueOf(entry.online());
        String max = String.valueOf(entry.max());
        return switch (entry.state()) {
            case CURRENT ->
                row(Material.PLAYER_HEAD, entry, new TextData(LobbySwitcherMessages.STATE_CURRENT, online, max));
            case FULL ->
                row(Material.RED_CONCRETE, entry, new TextData(LobbySwitcherMessages.STATE_FULL, online, max));
            case NOT_READY ->
                row(Material.YELLOW_CONCRETE, entry, new TextData(LobbySwitcherMessages.STATE_NOT_READY));
            case JOINABLE ->
                row(Material.LIME_CONCRETE, entry, new TextData(LobbySwitcherMessages.ENTRY_COUNT, online, max));
            case GONE, ERROR ->
                throw new IllegalStateException("a listed lobby cannot be " + entry.state());
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

        EnglishWithoutLocale(InventoryType type) {
            super(type);
        }

        @Override
        public Inventory getInventory(@Nullable Locale locale) {
            return super.getInventory(locale == null ? Locale.ENGLISH : locale);
        }
    }
}
