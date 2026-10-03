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
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.SpawnReturn;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The lobby's navigator: a feather in hotbar slot 4 that opens one of two Aves-built inventories,
 * each shared by every player who gets it: the public one, or the team one that adds the
 * permissioned {@code BUILD} destination fixed in {@link Destination}. Opening picks by permission.
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

    private final EventNode<Event> titan;
    private final Deliver deliver;
    private final FeatureFlags featureFlags;
    private final PermissionService permissions;
    private final SharedNavigator publicNavigator = new SharedNavigator(false);
    private final SharedNavigator teamNavigator = new SharedNavigator(true);
    // A Provider, not a bean: the spawn column sits before this one in module order (spawn ->
    // hotbar -> navigator item), so requiring SpawnReturn here would close a cycle.
    private final Provider<SpawnReturn> spawnReturnProvider;
    private SpawnReturn spawnReturn;
    private FeatureNode node;

    public NavigatorModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Deliver deliver, FeatureFlags featureFlags, PermissionService permissions, Provider<SpawnReturn> spawnReturnProvider) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.featureFlags = Objects.requireNonNull(featureFlags, "featureFlags must not be null");
        this.permissions = Objects.requireNonNull(permissions, "permissions must not be null");
        this.spawnReturnProvider = Objects.requireNonNull(spawnReturnProvider, "spawnReturnProvider must not be null");
    }

    @PostConstruct
    void start() {
        this.spawnReturn = resolveSpawnReturn();
        // Listener-less: only attached so this feature shows up in the fixed EVENT_PRIORITY order
        // and the leak test; Aves handles every inventory click itself.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY);
        this.publicNavigator.applyLayoutIfChanged(this.featureFlags, this::toAvesLayout);
        this.teamNavigator.applyLayoutIfChanged(this.featureFlags, this::toAvesLayout);
        this.publicNavigator.register();
        this.teamNavigator.register();
    }

    @PreDestroy
    void stop() {
        this.node.close();
        this.publicNavigator.unregister();
        this.teamNavigator.unregister();
    }

    void open(Player player) {
        SharedNavigator navigator = isAllowed(player, Destination.BUILD) ? this.teamNavigator : this.publicNavigator;
        navigator.applyLayoutIfChanged(this.featureFlags, this::toAvesLayout);
        player.openInventory(navigator.inventory());
    }

    // Test-only: lets a leak test assert the listener count on each Aves event node stays constant
    // across opens.
    Inventory publicInventory() {
        return this.publicNavigator.inventory();
    }

    Inventory teamInventory() {
        return this.teamNavigator.inventory();
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

    private InventoryLayout toAvesLayout(List<Destination> visible) {
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_1_ROW);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            layout.setItem(slot, BLANK);
        }
        for (Destination destination : visible) {
            layout.setItem(destination.slot(), destination.item(), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                if (!isAllowed(player, destination)) {
                    player.closeInventory();
                    return;
                }
                this.deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(destination.task()).player(player).build());
                player.closeInventory();
            });
        }
        addSpawnEntry(layout);
        return layout;
    }

    // Spawn is a fixed entry, not a Destination: it returns the player instead of redirecting them.
    private void addSpawnEntry(InventoryLayout layout) {
        layout.setItem(SPAWN_SLOT, SPAWN_ITEM, (player, clickedSlot, click, stack, result) -> {
            result.accept(ClickHolder.cancelClick());
            this.spawnReturn.sendToSpawnAndTell(player);
            player.closeInventory();
        });
    }
}
