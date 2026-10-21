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

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.SpawnReturn;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalApproach;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one of the Aves-built inventories,
 * each shared by every player who gets it. A menu is fixed by the permissioned destinations
 * ({@code SLENDER}, {@code BOUNCE}, {@code BUILD}, see {@link Destination}) a player holds,
 * so there is one per subset of them and language. Opening picks by permission.
 *
 * <p>A minigame's left click joins its best ready service (see {@link QuickJoin}), its right click
 * lists the services (see {@link ServerPicker}); a server connects on a left or right click. A
 * shift
 * click walks to the lobby's portal for the entry when there is one. Every other click type is
 * cancelled and does nothing (see {@link ClickAction}).
 *
 * <p>Aves maps its click listener directly onto each built {@code GlobalInventoryBuilder} inventory
 * and dispatches it before any regular event node's listeners, so this click handling always
 * completes before another feature could cancel the event first.
 */
@Singleton
public final class NavigatorModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 400;

    private static final String ID = "navigator";
    private static final ItemStack SPAWN_ITEM = ItemStack.builder(Material.COMPASS).customName(MiniMessage.miniMessage().deserialize("<!i><gradient:#00ddff:#ffffff>Spawn</gradient>")).build();
    private static final ItemStack BLANK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN);

    private final EventNode<Event> titan;
    private final Deliver deliver;
    private final PermissionService permissions;
    // One shared inventory per menu and language: the title and the click lines are translated, and a
    // shared inventory shows the same stacks to everyone who opens it.
    private final Map<Menu, SharedNavigator> menus = new HashMap<>();
    private final Telemetry telemetry;
    private final NavigatorTelemetry navigatorTelemetry;
    // A Provider, not a bean: the spawn column sits before this one in module order (spawn ->
    // hotbar -> navigator item), so requiring SpawnReturn here would close a cycle.
    private final Provider<SpawnReturn> spawnReturnProvider;
    private final LobbyPortals portals;
    private final Scheduler scheduler;
    private final Executor reads;
    private final TaskPlayers players;
    private final NavigatorSettings settings;
    // One server list per minigame and language, so each picker is in the language of its players.
    private final Map<Picker, SharedServerPicker> pickers = new HashMap<>();
    // Players whose click is waiting for the service read; a second click meanwhile is ignored.
    private final Set<UUID> choosing = new HashSet<>();
    private final AtomicBoolean reading = new AtomicBoolean();
    // The running services per destination, replaced as a whole by each refresh; the count line and
    // the click lines of every stack are both derived from it.
    private volatile Map<Destination, List<ServiceCount>> running = Map.of();
    private volatile boolean stopped;
    private SpawnReturn spawnReturn;
    private FeatureNode node;
    private Task refreshTask;

    public NavigatorModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Deliver deliver, PermissionService permissions, Provider<SpawnReturn> spawnReturnProvider, LobbyPortals portals, Telemetry telemetry, Optional<PlayerCounts> counts, Scheduler scheduler, @Named(NavigatorBeans.READS) Executor reads, NavigatorSettings settings, NavigatorMessages messages) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.permissions = Objects.requireNonNull(permissions, "permissions must not be null");
        // Injected so the translations are registered before this module renders its first line.
        Objects.requireNonNull(messages, "messages must not be null");
        this.spawnReturnProvider = Objects.requireNonNull(spawnReturnProvider, "spawnReturnProvider must not be null");
        this.portals = Objects.requireNonNull(portals, "portals must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.reads = Objects.requireNonNull(reads, "reads must not be null");
        this.players = new TaskPlayers(Objects.requireNonNull(counts, "counts must not be null"));
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.navigatorTelemetry = new NavigatorTelemetry(this.telemetry);
        for (Destination destination : ServerPicker.DESTINATIONS) {
            for (Locale language : LANGUAGES) {
                this.pickers.put(new Picker(destination, language), new SharedServerPicker(language));
            }
        }
    }

    @PostConstruct
    void start() {
        this.spawnReturn = resolveSpawnReturn();
        // Listener-less: only attached so this feature shows up in the fixed EVENT_PRIORITY order
        // and the leak test; Aves handles every inventory click itself.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry);
        // Built here, not in the constructor: the title is translated, and the translations are
        // registered when the messages bean starts, which happens before this module does.
        for (Set<Destination> granted : grantCombinations()) {
            for (Locale language : LANGUAGES) {
                SharedNavigator navigator = new SharedNavigator(NavigatorMessages.title(language), granted, this.telemetry);
                navigator.applyLayout(layoutFor(language));
                navigator.register();
                this.menus.put(new Menu(granted, language), navigator);
            }
        }
        // Tick-based on purpose: Minestom's time-based schedules run on a wall-clock timer outside the tick.
        TaskSchedule period = TaskSchedule.tick(Math.multiplyExact(this.settings.refreshSeconds(), ServerFlag.SERVER_TICKS_PER_SECOND));
        this.refreshTask = this.scheduler.scheduleTask(this::refreshCounts, TaskSchedule.nextTick(), period);
    }

    @PreDestroy
    void stop() {
        this.stopped = true;
        this.refreshTask.cancel();
        this.node.close();
        this.menus.values().forEach(SharedNavigator::unregister);
        this.pickers.values().forEach(SharedServerPicker::unregister);
    }

    void open(Player player) {
        Set<Destination> granted = grantedBy(player);
        Menu menu = new Menu(granted, languageOf(player));
        SharedNavigator navigator = this.menus.get(menu);
        // The destinations plus the fixed spawn entry.
        int entries = Destination.visible(granted).size() + 1;
        this.navigatorTelemetry.open(player.getUuid(), granted.contains(Destination.BUILD), entries, () -> {
            // A portal added since the last refresh must already show in the lines when the menu opens.
            navigator.showItems(destination -> itemOf(destination, menu.language()));
            player.openInventory(navigator.inventory());
        });
    }

    // Test-only: the live menu inventories for one language, so a leak test can read their event
    // nodes and stacks. The public menu is the one with no permissioned destination granted, the
    // team menu the one with all of them. English is the language a player without a locale gets.
    Inventory publicInventory() {
        return publicInventory(Locale.ENGLISH);
    }

    Inventory teamInventory() {
        return teamInventory(Locale.ENGLISH);
    }

    Inventory publicInventory(Locale language) {
        return inventory(Set.of(), language);
    }

    Inventory teamInventory(Locale language) {
        return inventory(Set.copyOf(Destination.permissioned()), language);
    }

    // Test-only seam: the shared menu for one grant set and language, so a test can count them.
    Inventory inventory(Set<Destination> granted, Locale language) {
        return this.menus.get(new Menu(granted, language)).inventory();
    }

    // The permissioned destinations this player may use right now; NOT_SET counts as not granted.
    private Set<Destination> grantedBy(Player player) {
        return Destination.permissioned().stream().filter(destination -> isAllowed(player, destination)).collect(Collectors.toUnmodifiableSet());
    }

    // Every subset of the permissioned destinations: each is one menu a player can be shown.
    private static List<Set<Destination>> grantCombinations() {
        List<Destination> permissioned = Destination.permissioned();
        List<Set<Destination>> combinations = new ArrayList<>();
        for (int mask = 0; mask < 1 << permissioned.size(); mask++) {
            Set<Destination> granted = EnumSet.noneOf(Destination.class);
            for (int bit = 0; bit < permissioned.size(); bit++) {
                if ((mask & 1 << bit) != 0) {
                    granted.add(permissioned.get(bit));
                }
            }
            combinations.add(Set.copyOf(granted));
        }
        return combinations;
    }

    // A player whose language has no bundle reads English.
    private static Locale languageOf(Player player) {
        String language = player.getLocale().getLanguage();
        return LANGUAGES.stream().filter(supported -> supported.getLanguage().equals(language)).findFirst().orElse(Locale.ENGLISH);
    }

    // The read may block on the network, so it runs on the executor; the inventories are only
    // touched from a scheduler task. A period that finds the previous read running is skipped.
    private void refreshCounts() {
        if (!this.reading.compareAndSet(false, true)) {
            return;
        }
        try {
            this.reads.execute(() -> {
                try {
                    Map<Destination, List<ServiceCount>> read = new EnumMap<>(Destination.class);
                    for (Destination destination : Destination.values()) {
                        read.put(destination, this.players.services(destination.task()).orElse(List.of()));
                    }
                    this.scheduler.scheduleNextTick(() -> showCounts(read));
                } finally {
                    this.reading.set(false);
                }
            });
        } catch (RuntimeException e) {
            this.reading.set(false);
        }
    }

    private void showCounts(Map<Destination, List<ServiceCount>> read) {
        if (this.stopped) {
            return;
        }
        this.running = read;
        this.menus.forEach((menu, navigator) -> navigator.showItems(destination -> itemOf(destination, menu.language())));
    }

    // The lines follow the last refresh, as the count does; the portal is looked up now, so a portal
    // added or removed shows up as soon as the menu is opened.
    private ItemStack itemOf(Destination destination, Locale language) {
        List<ServiceCount> services = this.running.getOrDefault(destination, List.of());
        List<ClickAction> actions = ClickAction.hints(ServerPicker.DESTINATIONS.contains(destination), portalFor(destination).isPresent());
        return destination.item(NavigatorMessages.modeLore(destination.gradient(), TaskPlayers.online(services), TaskPlayers.capacity(services), actions, language));
    }

    private SpawnReturn resolveSpawnReturn() {
        SpawnReturn resolved;
        try {
            resolved = this.spawnReturnProvider.get();
        } catch (RuntimeException cause) {
            throw new IllegalStateException("The navigator needs SpawnReturn from the spawn column, but it could not be resolved", cause);
        }
        if (resolved == null) {
            throw new IllegalStateException("The navigator needs SpawnReturn from the spawn column, but it could not be resolved");
        }
        return resolved;
    }

    // NOT_SET counts as not granted, like DENIED (lobby-permissions).
    private boolean isAllowed(Player player, Destination destination) {
        String permission = destination.permission();
        return permission == null || this.permissions.check(player.getUuid(), permission) == PermissionResult.ALLOWED;
    }

    private Function<List<Destination>, InventoryLayout> layoutFor(Locale language) {
        return visible -> toAvesLayout(visible, language);
    }

    private InventoryLayout toAvesLayout(List<Destination> visible, Locale language) {
        InventoryLayout layout = InventoryLayout.fromType(NavigatorLayout.TYPE);
        for (int slot = 0; slot < NavigatorLayout.SIZE; slot++) {
            layout.setItem(slot, NavigatorLayout.isFrame(slot) ? frameItem(slot) : BLANK);
        }
        for (Destination destination : visible) {
            layout.setItem(destination.slot(), itemOf(destination, language), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                clickDestination(player, destination, click, language);
            });
        }
        addSpawnEntry(layout);
        return layout;
    }

    private static ItemStack frameItem(int slot) {
        return ItemStack.builder(NavigatorLayout.frameMaterial(slot)).customName(Component.empty()).build();
    }

    // Decides the click first: a join or the server list needs the services read, a connect or a
    // walk to the portal happens at once. A click no action is named for does nothing.
    private void clickDestination(Player player, Destination destination, Click click, Locale language) {
        Optional<Portal> portal = portalFor(destination);
        Optional<ClickAction> named = ClickAction.of(ServerPicker.DESTINATIONS.contains(destination), click, portal.isPresent());
        if (named.isEmpty()) {
            return;
        }
        ClickAction action = named.get();
        if (action == ClickAction.QUICK_JOIN || action == ClickAction.PICK_SERVER) {
            readServices(player, destination, (viewer, running) -> conclude(viewer, destination, action, language, running));
            return;
        }
        this.navigatorTelemetry.select(player.getUuid(), destination.name(), () -> {
            if (!isAllowed(player, destination)) {
                player.closeInventory();
                return NavigatorTelemetry.Selection.DENIED;
            }
            if (action == ClickAction.PORTAL && portal.isPresent()) {
                player.closeInventory();
                player.teleport(PortalApproach.standingBefore(portal.get(), player.getPosition()));
                return NavigatorTelemetry.Selection.TELEPORTED;
            }
            return connectToTask(player, destination);
        });
    }

    private Optional<Portal> portalFor(Destination destination) {
        return this.portals.portals().stream().filter(portal -> portal.task().equals(destination.task())).findFirst();
    }

    private NavigatorTelemetry.Selection connectToTask(Player player, Destination destination) {
        this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(destination.task()).player(player).build());
        player.closeInventory();
        return NavigatorTelemetry.Selection.SENT;
    }

    // The services are read off the tick, and the decision follows on the next tick with that reading,
    // so neither the join, the list nor a pick ever acts on a stale or missing reading. An empty
    // Optional means the read failed. A second click by the same player meanwhile is ignored.
    private void readServices(Player player, Destination destination, BiConsumer<Player, Optional<List<ServiceCount>>> decision) {
        if (!this.choosing.add(player.getUuid())) {
            return;
        }
        try {
            this.reads.execute(() -> {
                Optional<List<ServiceCount>> running = this.players.services(destination.task());
                this.scheduler.scheduleNextTick(() -> {
                    this.choosing.remove(player.getUuid());
                    if (this.stopped || !player.isOnline()) {
                        return;
                    }
                    decision.accept(player, running);
                });
            });
        } catch (RuntimeException e) {
            this.choosing.remove(player.getUuid());
            decision.accept(player, Optional.empty());
        }
    }

    private void conclude(Player player, Destination destination, ClickAction action, Locale language, Optional<List<ServiceCount>> read) {
        this.navigatorTelemetry.select(player.getUuid(), destination.name(), () -> {
            if (!isAllowed(player, destination)) {
                player.closeInventory();
                return NavigatorTelemetry.Selection.DENIED;
            }
            if (read.isEmpty()) {
                player.sendMessage(ServerPicker.unavailable(language));
                return NavigatorTelemetry.Selection.REFUSED;
            }
            if (action == ClickAction.QUICK_JOIN) {
                Optional<ServiceCount> service = QuickJoin.pick(read.get());
                if (service.isPresent()) {
                    return joinService(player, service.get());
                }
            }
            openList(player, destination, language, PickerSort.NAME, 0, read.get());
            return NavigatorTelemetry.Selection.PICKER;
        });
    }

    // A sort or page change reads the services again and shows the list it asks for. It is not a
    // selection, so it is not recorded as one.
    private void relist(Player player, Destination destination, Locale language, PickerSort sort, int page) {
        readServices(player, destination, (viewer, read) -> {
            if (!isAllowed(viewer, destination)) {
                viewer.closeInventory();
                return;
            }
            if (read.isEmpty()) {
                viewer.sendMessage(ServerPicker.unavailable(language));
                return;
            }
            openList(viewer, destination, language, sort, page, read.get());
        });
    }

    private void openList(Player player, Destination destination, Locale language, PickerSort sort, int page, List<ServiceCount> running) {
        player.openInventory(this.pickers.get(new Picker(destination, language)).show(destination, sort, page, running, new PickerClicks(destination, language)));
    }

    // A picked entry is joined only if it still qualifies now, so the list is read again first.
    private void pick(Player player, Destination destination, Locale language, ServiceCount picked) {
        readServices(player, destination, (viewer, read) -> decidePick(viewer, destination, language, picked, read));
    }

    private void decidePick(Player player, Destination destination, Locale language, ServiceCount picked, Optional<List<ServiceCount>> read) {
        this.navigatorTelemetry.select(player.getUuid(), destination.name(), () -> {
            if (!isAllowed(player, destination)) {
                player.closeInventory();
                return NavigatorTelemetry.Selection.DENIED;
            }
            if (read.isEmpty()) {
                player.sendMessage(ServerPicker.unavailable(language));
                return NavigatorTelemetry.Selection.REFUSED;
            }
            ServiceCount current = currentOf(picked, read.get());
            if (!ServerPicker.joinable(current)) {
                player.sendMessage(ServerPicker.refusal(current, language));
                return NavigatorTelemetry.Selection.REFUSED;
            }
            return joinService(player, current);
        });
    }

    // The picked service as it runs now; one that has stopped reads as not ready, which is refused.
    private static ServiceCount currentOf(ServiceCount picked, List<ServiceCount> running) {
        return running.stream().filter(service -> service.name().equals(picked.name())).findFirst().orElse(new ServiceCount(picked.name(), 0, 0));
    }

    private NavigatorTelemetry.Selection joinService(Player player, ServiceCount service) {
        this.deliver.sendPlayer(player, DeliverComponent.serverBuilder().serverName(service.name()).player(player).build());
        player.closeInventory();
        return NavigatorTelemetry.Selection.SENT;
    }

    /** One shared inventory: the permissioned destinations it shows, in one language. */
    private record Menu(Set<Destination> granted, Locale language) {
    }

    /** One shared server list: the minigame it lists, in one language. */
    private record Picker(Destination destination, Locale language) {
    }

    /** The clicks of one server list, which know the minigame and the language it lists. */
    private final class PickerClicks implements SharedServerPicker.Clicks {

        private final Destination destination;
        private final Locale language;

        PickerClicks(Destination destination, Locale language) {
            this.destination = destination;
            this.language = language;
        }

        @Override
        public void pick(Player player, ServiceCount service) {
            NavigatorModule.this.pick(player, this.destination, this.language, service);
        }

        @Override
        public void sort(Player player, PickerSort current) {
            relist(player, this.destination, this.language, current.next(), 0);
        }

        @Override
        public void turn(Player player, PickerSort current, int page) {
            relist(player, this.destination, this.language, current, page);
        }
    }

    // Spawn is a fixed entry, not a Destination: it returns the player instead of redirecting them.
    private void addSpawnEntry(InventoryLayout layout) {
        layout.setItem(NavigatorLayout.SPAWN_SLOT, SPAWN_ITEM, (player, clickedSlot, click, stack, result) -> {
            result.accept(ClickHolder.cancelClick());
            this.navigatorTelemetry.select(player.getUuid(), NavigatorTelemetry.SPAWN_DESTINATION, () -> {
                this.spawnReturn.sendToSpawnAndTell(player, SpawnReturn.Source.NAVIGATOR);
                player.closeInventory();
                return NavigatorTelemetry.Selection.SPAWN;
            });
        });
    }
}
