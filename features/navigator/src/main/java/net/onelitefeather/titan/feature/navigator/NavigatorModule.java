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
import java.time.Clock;
import java.time.Duration;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.inventory.AbstractInventory;
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
import net.onelitefeather.titan.core.portal.TaskJoin;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one of the Aves-built inventories,
 * each shared by every player who gets it. The main menu is fixed by the live permissioned
 * destinations ({@code SLENDER}, {@code BOUNCE}, {@code TAMIAS}, {@code BUILD}, see
 * {@link Destination}) a player holds, and by whether a player holds any staging permission, which
 * shows the staging hub. The staging submenu is fixed by the staging permissions held. Opening
 * picks
 * by permission.
 *
 * <p>A minigame's left click joins its best ready service (see {@link TaskJoin}), its right click
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
    // One shared submenu per set of staging grants and language, opened by the staging hub.
    private final Map<Submenu, SharedNavigator> submenus = new HashMap<>();
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
    // The staging entries start on demand: the click is decided by the start logic, and the idle stop
    // runs on the read thread of each refresh. Without a provider, every staging request is refused.
    private final OnDemandServices services;
    private final StagingStarts<Player> starts;
    private final StagingIdle idle;
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

    public NavigatorModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Deliver deliver, PermissionService permissions, Provider<SpawnReturn> spawnReturnProvider, LobbyPortals portals, Telemetry telemetry, Optional<PlayerCounts> counts, Scheduler scheduler, @Named(NavigatorBeans.READS) Executor reads, NavigatorSettings settings, NavigatorMessages messages, Optional<OnDemandServices> onDemand, Clock clock) {
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
        this.services = Objects.requireNonNull(onDemand, "onDemand must not be null").orElse(NoOnDemandServices.INSTANCE);
        this.navigatorTelemetry = new NavigatorTelemetry(this.telemetry);
        StagingPorts stagingPorts = new StagingPorts(this::isAllowed, this::connectToTask);
        this.starts = new StagingStarts<>(this.services, Objects.requireNonNull(clock, "clock must not be null"), stagingPorts, stagingPorts, this.navigatorTelemetry);
        this.idle = new StagingIdle(this.services, clock, Duration.ofSeconds(settings.stagingIdleSeconds()), this.navigatorTelemetry);
        for (Destination destination : ServerPicker.DESTINATIONS) {
            for (Locale language : LANGUAGES) {
                this.pickers.put(new Picker(destination, language), new SharedServerPicker(language));
            }
        }
    }

    @PostConstruct
    void start() {
        this.spawnReturn = resolveSpawnReturn();
        // Aves handles every inventory click itself. The one listener is attached once here, not per
        // player, and drops a player who leaves while waiting for a staging service.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerDisconnectEvent.class, event -> this.starts.disconnected(event.getPlayer()));
        // Built here, not in the constructor: the title is translated, and the translations are
        // registered when the messages bean starts, which happens before this module does.
        for (Set<Destination> granted : grantCombinations(Destination.livePermissioned())) {
            for (boolean stagingHub : List.of(false, true)) {
                for (Locale language : LANGUAGES) {
                    SharedNavigator navigator = new SharedNavigator(NavigatorMessages.title(language), NavigatorLayout.TYPE, Destination.visible(granted), this.telemetry);
                    navigator.applyLayout(visible -> toAvesLayout(visible, stagingHub, language));
                    navigator.register();
                    this.menus.put(new Menu(granted, stagingHub, language), navigator);
                }
            }
        }
        for (Set<Destination> granted : grantCombinations(Destination.stagingEntries())) {
            for (Locale language : LANGUAGES) {
                SharedNavigator submenu = new SharedNavigator(StagingMenu.title(language), StagingMenu.TYPE, Destination.stagingVisible(granted), this.telemetry);
                submenu.applyLayout(visible -> toStagingLayout(visible, language));
                submenu.register();
                this.submenus.put(new Submenu(granted, language), submenu);
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
        this.submenus.values().forEach(SharedNavigator::unregister);
        this.pickers.values().forEach(SharedServerPicker::unregister);
    }

    void open(Player player) {
        Set<Destination> granted = grantedBy(player, Destination.livePermissioned());
        boolean stagingHub = !grantedBy(player, Destination.stagingEntries()).isEmpty();
        Menu menu = new Menu(granted, stagingHub, languageOf(player));
        SharedNavigator navigator = this.menus.get(menu);
        // The destinations, the hub when it shows, plus the fixed spawn entry.
        int entries = Destination.visible(granted).size() + 1 + (stagingHub ? 1 : 0);
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
        return inventory(Set.of(), false, language);
    }

    Inventory teamInventory(Locale language) {
        return inventory(Set.copyOf(Destination.livePermissioned()), true, language);
    }

    // Test-only seam: the main menu for one grant set and language, without the staging hub.
    Inventory inventory(Set<Destination> granted, Locale language) {
        return inventory(granted, false, language);
    }

    // Test-only seam: the shared main menu for one grant set, staging hub flag and language.
    Inventory inventory(Set<Destination> granted, boolean stagingHub, Locale language) {
        return this.menus.get(new Menu(granted, stagingHub, language)).inventory();
    }

    // Test-only seam: the shared submenu for one set of staging grants and language.
    Inventory stagingInventory(Set<Destination> granted, Locale language) {
        return this.submenus.get(new Submenu(granted, language)).inventory();
    }

    // The destinations of the list this player may use right now; NOT_SET counts as not granted.
    private Set<Destination> grantedBy(Player player, List<Destination> destinations) {
        return destinations.stream().filter(destination -> isAllowed(player, destination)).collect(Collectors.toUnmodifiableSet());
    }

    // Every subset of the given destinations: each is one grant set a menu can be shown for.
    private static List<Set<Destination>> grantCombinations(List<Destination> permissioned) {
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
    static Locale languageOf(Player player) {
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
                    Map<Destination, Optional<List<ServiceState>>> staging = readStaging();
                    this.idle.refresh(staging);
                    this.scheduler.scheduleNextTick(() -> showCounts(read, staging));
                } finally {
                    this.reading.set(false);
                }
            });
        } catch (RuntimeException e) {
            this.reading.set(false);
        }
    }

    // The services of every staging entry, read off the tick with the counts of this cycle.
    private Map<Destination, Optional<List<ServiceState>>> readStaging() {
        Map<Destination, Optional<List<ServiceState>>> staging = new EnumMap<>(Destination.class);
        for (Destination destination : Destination.values()) {
            if (destination.onDemand()) {
                staging.put(destination, StagingStarts.read(this.services, destination));
            }
        }
        return staging;
    }

    private void showCounts(Map<Destination, List<ServiceCount>> read, Map<Destination, Optional<List<ServiceState>>> staging) {
        if (this.stopped) {
            return;
        }
        this.running = read;
        this.menus.forEach((menu, navigator) -> navigator.showItems(destination -> itemOf(destination, menu.language())));
        this.submenus.forEach((submenu, navigator) -> navigator.showItems(destination -> itemOf(destination, submenu.language())));
        this.starts.refresh(staging);
    }

    // The lines follow the last refresh, as the count does; the portal is looked up now, so a portal
    // added or removed shows up as soon as the menu is opened.
    private ItemStack itemOf(Destination destination, Locale language) {
        List<ServiceCount> services = this.running.getOrDefault(destination, List.of());
        List<ClickAction> actions = ClickAction.hints(ServerPicker.DESTINATIONS.contains(destination), portalFor(destination).isPresent());
        List<Component> lore = new ArrayList<>(NavigatorMessages.modeLore(destination.gradient(), TaskPlayers.online(services), TaskPlayers.capacity(services), actions, language));
        List<String> hints = new ArrayList<>();
        if (destination.onDemand()) {
            hints.add(NavigatorMessages.STAGING_ON_DEMAND_HINT);
        }
        if (destination.hint() != null) {
            hints.add(destination.hint());
        }
        if (!hints.isEmpty()) {
            lore.add(Component.empty());
            hints.forEach(hint -> lore.add(NavigatorMessages.hintLine(hint, language)));
        }
        return destination.item(List.copyOf(lore));
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

    private InventoryLayout toAvesLayout(List<Destination> visible, boolean stagingHub, Locale language) {
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
        if (stagingHub) {
            layout.setItem(NavigatorLayout.STAGING_HUB_SLOT, StagingMenu.hub(language), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                openStaging(player);
            });
        }
        addSpawnEntry(layout);
        return layout;
    }

    private InventoryLayout toStagingLayout(List<Destination> visible, Locale language) {
        InventoryLayout layout = InventoryLayout.fromType(StagingMenu.TYPE);
        for (int slot = 0; slot < StagingMenu.SIZE; slot++) {
            layout.setItem(slot, BLANK);
        }
        for (Destination destination : visible) {
            layout.setItem(destination.slot(), itemOf(destination, language), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                clickDestination(player, destination, click, language);
            });
        }
        layout.setItem(StagingMenu.BACK_SLOT, StagingMenu.back(language), (player, clickedSlot, click, stack, result) -> {
            result.accept(ClickHolder.cancelClick());
            open(player);
        });
        return layout;
    }

    // The submenu follows the staging grants held now. A player who holds none gets the navigator back.
    private void openStaging(Player player) {
        Set<Destination> granted = grantedBy(player, Destination.stagingEntries());
        if (granted.isEmpty()) {
            open(player);
            return;
        }
        player.openInventory(this.submenus.get(new Submenu(granted, languageOf(player))).inventory());
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
        if (action == ClickAction.CONNECT && destination.onDemand()) {
            readThen(player, () -> StagingStarts.read(this.services, destination), (viewer, read) -> decideStaging(viewer, destination, read));
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

    // Every entry that connects leaves the menu here, staging entries included.
    private NavigatorTelemetry.Selection connectToTask(Player player, Destination destination) {
        this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(destination.task()).player(player).build());
        player.closeInventory();
        return NavigatorTelemetry.Selection.SENT;
    }

    private void readServices(Player player, Destination destination, BiConsumer<Player, Optional<List<ServiceCount>>> decision) {
        readThen(player, () -> this.players.services(destination.task()), decision);
    }

    // A staging click connects, starts or waits as the start logic decides; the decision is recorded
    // as the click's outcome, and a waiting player's inventory closes so the action bar shows.
    private void decideStaging(Player player, Destination destination, Optional<List<ServiceState>> read) {
        this.navigatorTelemetry.select(player.getUuid(), destination.name(), () -> {
            StagingStarts.Outcome outcome = this.starts.click(player, destination, read);
            return switch (outcome) {
                case CONNECTED -> NavigatorTelemetry.Selection.SENT;
                case WAITING -> {
                    player.closeInventory();
                    yield NavigatorTelemetry.Selection.PENDING;
                }
                case DENIED -> {
                    player.closeInventory();
                    yield NavigatorTelemetry.Selection.DENIED;
                }
                case REFUSED -> NavigatorTelemetry.Selection.REFUSED;
            };
        });
    }

    // The services are read off the tick, and the decision follows on the next tick with that reading,
    // so neither the join, the list, a pick nor a staging start ever acts on a stale or missing reading.
    // An empty Optional means the read failed. A second click by the same player meanwhile is ignored.
    private <T> void readThen(Player player, Supplier<Optional<T>> read, BiConsumer<Player, Optional<T>> decision) {
        if (!this.choosing.add(player.getUuid())) {
            return;
        }
        try {
            this.reads.execute(() -> {
                Optional<T> reading = read.get();
                this.scheduler.scheduleNextTick(() -> {
                    this.choosing.remove(player.getUuid());
                    if (this.stopped || !player.isOnline()) {
                        return;
                    }
                    decision.accept(player, reading);
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
                Optional<ServiceCount> service = TaskJoin.pick(read.get());
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

    // A read that completes after the player closed the menu must not bring the list back, so the list
    // opens only while one of this module's inventories is still the one the player has open.
    private void openList(Player player, Destination destination, Locale language, PickerSort sort, int page, List<ServiceCount> running) {
        if (!showsOwnInventory(player)) {
            return;
        }
        player.openInventory(this.pickers.get(new Picker(destination, language)).show(destination, sort, page, running, new PickerClicks(destination, language)));
    }

    private boolean showsOwnInventory(Player player) {
        AbstractInventory open = player.getOpenInventory();
        return open != null && (this.menus.values().stream().anyMatch(navigator -> navigator.inventory() == open) || this.pickers.values().stream().anyMatch(picker -> picker.owns(open)));
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
            if (!TaskJoin.joinable(current)) {
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

    /**
     * One shared main menu: the live destinations it shows, whether the staging hub shows, and the
     * language.
     */
    private record Menu(Set<Destination> granted, boolean stagingHub, Locale language) {
    }

    /** One shared staging submenu: the staging grants it shows, in one language. */
    private record Submenu(Set<Destination> granted, Locale language) {
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
        public void back(Player player) {
            // The same path as the feather, so the menu follows the permissions and language now.
            NavigatorModule.this.open(player);
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
