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
import java.util.EnumMap;
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
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.SpawnReturn;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalApproach;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one of two Aves-built inventories,
 * each shared by every player who gets it: the public one, or the team one that adds the
 * permissioned {@code BUILD} destination fixed in {@link Destination}. Opening picks by permission.
 *
 * <p>A right click on a destination walks the player to the lobby's portal for that task instead
 * of connecting, when the map has one; a left click on a mode that runs on several services opens
 * the list of them instead (see {@link ServerPicker}); any other click connects.
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
    private static final int SLOT_COUNT = 9;
    private static final int SPAWN_SLOT = 2;
    private static final ItemStack SPAWN_ITEM = ItemStack.builder(Material.COMPASS).customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Spawn</aqua>")).build();
    private static final ItemStack BLANK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN);

    private final EventNode<Event> titan;
    private final Deliver deliver;
    private final FeatureFlags featureFlags;
    private final PermissionService permissions;
    private final NavigatorMessages messages;
    // One shared inventory per menu and language: the click hint is translated, and a shared
    // inventory shows the same stacks to everyone who opens it.
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
    private final Map<Destination, SharedServerPicker> pickers = new EnumMap<>(Destination.class);
    // Players whose left click is waiting for the service read; a second click meanwhile is ignored.
    private final Set<UUID> choosing = new HashSet<>();
    private final AtomicBoolean reading = new AtomicBoolean();
    // The running services per destination, replaced as a whole by each refresh; the count line and
    // the click hint of every stack are both derived from it.
    private volatile Map<Destination, List<ServiceCount>> running = Map.of();
    private volatile boolean stopped;
    private SpawnReturn spawnReturn;
    private FeatureNode node;
    private Task refreshTask;

    public NavigatorModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Deliver deliver, FeatureFlags featureFlags, PermissionService permissions, Provider<SpawnReturn> spawnReturnProvider, LobbyPortals portals, Telemetry telemetry, Optional<PlayerCounts> counts, Scheduler scheduler, @Named(NavigatorBeans.READS) Executor reads, NavigatorSettings settings, NavigatorMessages messages) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.featureFlags = Objects.requireNonNull(featureFlags, "featureFlags must not be null");
        this.permissions = Objects.requireNonNull(permissions, "permissions must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.spawnReturnProvider = Objects.requireNonNull(spawnReturnProvider, "spawnReturnProvider must not be null");
        this.portals = Objects.requireNonNull(portals, "portals must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.reads = Objects.requireNonNull(reads, "reads must not be null");
        this.players = new TaskPlayers(Objects.requireNonNull(counts, "counts must not be null"));
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.navigatorTelemetry = new NavigatorTelemetry(this.telemetry);
        for (Locale language : LANGUAGES) {
            this.menus.put(new Menu(false, language), new SharedNavigator(false, this.telemetry));
            this.menus.put(new Menu(true, language), new SharedNavigator(true, this.telemetry));
        }
        for (Destination destination : ServerPicker.DESTINATIONS) {
            this.pickers.put(destination, new SharedServerPicker());
        }
    }

    @PostConstruct
    void start() {
        this.spawnReturn = resolveSpawnReturn();
        // Listener-less: only attached so this feature shows up in the fixed EVENT_PRIORITY order
        // and the leak test; Aves handles every inventory click itself.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry);
        this.menus.forEach((menu, navigator) -> {
            navigator.applyLayoutIfChanged(this.featureFlags, layoutFor(menu.language()));
            navigator.register();
        });
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
        boolean team = isAllowed(player, Destination.BUILD);
        Menu menu = new Menu(team, languageOf(player));
        SharedNavigator navigator = this.menus.get(menu);
        // The destinations plus the fixed spawn entry.
        int entries = Destination.visible(this.featureFlags, team).size() + 1;
        this.navigatorTelemetry.open(player.getUuid(), team, entries, () -> {
            navigator.applyLayoutIfChanged(this.featureFlags, layoutFor(menu.language()));
            // A portal added since the last refresh must already show in the hint when the menu opens.
            navigator.showItems(destination -> itemOf(destination, menu.language()));
            player.openInventory(navigator.inventory());
        });
    }

    // Test-only: lets a leak test assert the listener count on each Aves event node stays constant
    // across opens. English is the language a player without a locale gets.
    Inventory publicInventory() {
        return inventory(false, Locale.ENGLISH);
    }

    Inventory teamInventory() {
        return inventory(true, Locale.ENGLISH);
    }

    private Inventory inventory(boolean team, Locale language) {
        return this.menus.get(new Menu(team, language)).inventory();
    }

    // A player with no locale yet, or one whose language has no bundle, reads English.
    private static Locale languageOf(Player player) {
        Locale locale = player.getLocale();
        return locale != null && Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH;
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
                        read.put(destination, this.players.services(destination.task()));
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

    // The picker hint follows the last refresh, as the count does; the portal is looked up now, so a
    // portal added or removed shows up as soon as the menu is opened.
    private ItemStack itemOf(Destination destination, Locale language) {
        List<ServiceCount> services = this.running.getOrDefault(destination, List.of());
        boolean serverPicker = ServerPicker.opensFor(destination, services);
        return destination.item(TaskPlayers.online(services), this.messages.clickHint(language, serverPicker, portalFor(destination).isPresent()));
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
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_1_ROW);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            layout.setItem(slot, BLANK);
        }
        for (Destination destination : visible) {
            layout.setItem(destination.slot(), itemOf(destination, language), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                if (!isRightClick(click) && ServerPicker.DESTINATIONS.contains(destination)) {
                    chooseServer(player, destination);
                    return;
                }
                this.navigatorTelemetry.select(player.getUuid(), destination.name(), () -> {
                    if (!isAllowed(player, destination)) {
                        player.closeInventory();
                        return NavigatorTelemetry.Selection.DENIED;
                    }
                    Optional<Portal> portal = isRightClick(click) ? portalFor(destination) : Optional.empty();
                    if (portal.isPresent()) {
                        player.closeInventory();
                        player.teleport(PortalApproach.standingBefore(portal.get(), player.getPosition()));
                        return NavigatorTelemetry.Selection.TELEPORTED;
                    }
                    return connectToTask(player, destination);
                });
            });
        }
        addSpawnEntry(layout);
        return layout;
    }

    // Shift keeps the button: a shift-right click is still a right click. Every other click type
    // connects, as the left click does.
    private static boolean isRightClick(Click click) {
        return click instanceof Click.Right || click instanceof Click.RightShift;
    }

    private Optional<Portal> portalFor(Destination destination) {
        return this.portals.portals().stream().filter(portal -> portal.task().equals(destination.task())).findFirst();
    }

    private NavigatorTelemetry.Selection connectToTask(Player player, Destination destination) {
        this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(destination.task()).player(player).build());
        player.closeInventory();
        return NavigatorTelemetry.Selection.SENT;
    }

    // The services are read off the tick; the decision follows on the next tick, so the list never
    // opens with a stale or empty reading.
    private void chooseServer(Player player, Destination destination) {
        if (!this.choosing.add(player.getUuid())) {
            return;
        }
        try {
            this.reads.execute(() -> {
                List<ServiceCount> running = this.players.services(destination.task());
                this.scheduler.scheduleNextTick(() -> conclude(player, destination, running));
            });
        } catch (RuntimeException e) {
            conclude(player, destination, List.of());
        }
    }

    private void conclude(Player player, Destination destination, List<ServiceCount> running) {
        this.choosing.remove(player.getUuid());
        if (this.stopped || !player.isOnline()) {
            return;
        }
        this.navigatorTelemetry.select(player.getUuid(), destination.name(), () -> {
            if (!isAllowed(player, destination)) {
                player.closeInventory();
                return NavigatorTelemetry.Selection.DENIED;
            }
            if (ServerPicker.opensFor(destination, running)) {
                player.openInventory(this.pickers.get(destination).show(ServerPicker.entries(running), (viewer, service) -> pick(viewer, destination, service)));
                return NavigatorTelemetry.Selection.PICKER;
            }
            return connectToTask(player, destination);
        });
    }

    private void pick(Player player, Destination destination, ServiceCount service) {
        this.navigatorTelemetry.select(player.getUuid(), destination.name(), () -> {
            if (!isAllowed(player, destination)) {
                player.closeInventory();
                return NavigatorTelemetry.Selection.DENIED;
            }
            if (!ServerPicker.joinable(service)) {
                player.sendMessage(ServerPicker.refusal(service));
                return NavigatorTelemetry.Selection.REFUSED;
            }
            this.deliver.sendPlayer(player, DeliverComponent.serverBuilder().serverName(service.name()).player(player).build());
            player.closeInventory();
            return NavigatorTelemetry.Selection.SENT;
        });
    }

    /** One shared inventory: the public or the team menu, in one language. */
    private record Menu(boolean team, Locale language) {
    }

    // Spawn is a fixed entry, not a Destination: it returns the player instead of redirecting them.
    private void addSpawnEntry(InventoryLayout layout) {
        layout.setItem(SPAWN_SLOT, SPAWN_ITEM, (player, clickedSlot, click, stack, result) -> {
            result.accept(ClickHolder.cancelClick());
            this.navigatorTelemetry.select(player.getUuid(), NavigatorTelemetry.SPAWN_DESTINATION, () -> {
                this.spawnReturn.sendToSpawnAndTell(player, SpawnReturn.Source.NAVIGATOR);
                player.closeInventory();
                return NavigatorTelemetry.Selection.SPAWN;
            });
        });
    }
}
