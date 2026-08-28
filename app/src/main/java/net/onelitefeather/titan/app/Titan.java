/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.app;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.butterfly.minestom.Butterfly;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.commands.StopCommand;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.app.listener.*;
import net.onelitefeather.titan.app.player.TitanPlayer;
import net.onelitefeather.titan.common.config.AppConfigProvider;
import net.onelitefeather.titan.common.deliver.DeliverProvider;
import net.onelitefeather.titan.common.event.EntityDismountEvent;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.resourcepack.PackFailureNotice;
import net.onelitefeather.titan.common.resourcepack.PackSlot;
import net.onelitefeather.titan.common.resourcepack.ResourcePackSeasonWatcher;
import net.onelitefeather.titan.common.resourcepack.ResourcePackService;
import net.onelitefeather.titan.common.resourcepack.ResourcePackSettings;
import net.onelitefeather.titan.common.resourcepack.ResourcePackSettingsProvider;
import net.onelitefeather.titan.common.utils.Cancelable;

import java.nio.file.Path;

public final class Titan {

    private final Path path;
    private final EventNode<Event> eventNode = EventNode.all("titan");
    private final Deliver deliver = DeliverProvider.create();
    private final MapProvider mapProvider;
    private final AppConfigProvider appConfigProvider;
    private final NavigationHelper navigationHelper;
    private final ResourcePackService resourcePackService;
    private final ResourcePackSeasonWatcher resourcePackSeasonWatcher;

    public Titan() {
        MinecraftServer.getConnectionManager().setPlayerProvider(TitanPlayer::new);
        this.path = Path.of("");
        BlockHandlerHelper.registerAll();
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.mapProvider = MapProvider.create(this.path, instance);
        this.appConfigProvider = AppConfigProvider.create(this.path);
        this.navigationHelper = NavigationHelper.instance(this.deliver);
        // Without a resource-packs.json this stays disabled and no listener is registered
        // below, so a lobby without a pack server behaves exactly as it did before.
        ResourcePackSettings resourcePackSettings = ResourcePackSettingsProvider.load(this.path);
        this.resourcePackService = ResourcePackService.create(resourcePackSettings).withCondition(PackSlot.BASE, new PackFailureNotice(PackSlot.BASE)).withCondition(PackSlot.SEASON, new PackFailureNotice(PackSlot.SEASON));
        this.resourcePackSeasonWatcher = ResourcePackSeasonWatcher.create(this.path, this.resourcePackService);
    }

    public void initialize() {
        initListeners();
        initCommands();
        Butterfly butterfly = Butterfly.create();
        butterfly.load();
        MinecraftServer.getSchedulerManager().buildShutdownTask(this::terminate);
        MinecraftServer.getSchedulerManager().buildShutdownTask(butterfly::terminate);
    }

    public void terminate() {
        this.resourcePackSeasonWatcher.close();
        this.resourcePackService.close();
    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new EndCommand());
        MinecraftServer.getCommandManager().register(new StopCommand());
    }

    private void initListeners() {

        this.eventNode.addListener(PickupItemEvent.class, Cancelable::cancel);
        this.eventNode.addListener(InventoryPreClickEvent.class, Cancelable::cancel);
        this.eventNode.addListener(PlayerBlockBreakEvent.class, Cancelable::cancel);
        this.eventNode.addListener(PlayerBlockPlaceEvent.class, Cancelable::cancel);
        this.eventNode.addListener(PlayerSwapItemEvent.class, Cancelable::cancel);
        this.eventNode.addListener(ItemDropEvent.class, Cancelable::cancel);

        this.eventNode.addListener(PlayerDeathEvent.class, new DeathListener());
        this.eventNode.addListener(EntityAttackEvent.class, new TickleListener(this.appConfigProvider.getAppConfig()));

        this.eventNode.addListener(PlayerBlockInteractEvent.class, new SitListener(this.appConfigProvider.getAppConfig()));
        this.eventNode.addListener(PlayerPacketEvent.class, new SitLeavePacketListener());
        this.eventNode.addListener(EntityDismountEvent.class, new SitDismountListener());
        this.eventNode.addListener(PlayerDisconnectEvent.class, new SitDisconnectListener());

        this.eventNode.addListener(PlayerUseItemEvent.class, new NavigationListener(this.navigationHelper));

        this.eventNode.addListener(PlayerStartFlyingWithElytraEvent.class, new ElytraStartFlyingListener());
        this.eventNode.addListener(PlayerStopFlyingWithElytraEvent.class, new ElytraStopFlyingListener());
        this.eventNode.addListener(PlayerUseItemEvent.class, new ElytraBoostListener(this.appConfigProvider.getAppConfig()));

        this.eventNode.addListener(PlayerRespawnEvent.class, new RespawnListener(this.navigationHelper));
        this.eventNode.addListener(PlayerMoveEvent.class, new PlayerMoveListener(this.appConfigProvider.getAppConfig(), this.mapProvider.getActiveLobby()));

        this.eventNode.addListener(AsyncPlayerConfigurationEvent.class, new PlayerConfigurationListener(this.mapProvider));
        this.eventNode.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(
                this.appConfigProvider.getAppConfig(), this.mapProvider.getActiveLobby(), this.navigationHelper));

        initResourcePackListeners();

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    /**
     * Registers the resource pack listeners and starts watching for season changes - but only
     * when a pack is actually configured. An unconfigured lobby registers nothing at all and
     * therefore cannot send a single pack packet.
     */
    private void initResourcePackListeners() {
        if (!this.resourcePackService.enabled()) {
            return;
        }
        this.eventNode.addListener(AsyncPlayerConfigurationEvent.class, new ResourcePackConfigurationListener(this.resourcePackService));
        this.eventNode.addListener(PlayerResourcePackStatusEvent.class, new ResourcePackStatusListener(this.resourcePackService));
        this.eventNode.addListener(PlayerDisconnectEvent.class, new ResourcePackDisconnectListener(this.resourcePackService));
        // Without this the season only ever changes on a restart: applySeason would have no
        // caller and an edited resource-packs.json would reach nobody who is already online.
        this.resourcePackSeasonWatcher.start();
    }

    /**
     * The resource pack delivery of this lobby.
     *
     * @return the resource pack service
     */
    public ResourcePackService resourcePackService() {
        return this.resourcePackService;
    }

    /**
     * The watcher that turns an edited {@code resource-packs.json} into a season change.
     *
     * @return the season watcher
     */
    public ResourcePackSeasonWatcher resourcePackSeasonWatcher() {
        return this.resourcePackSeasonWatcher;
    }

    public static Titan instance() {
        return new Titan();
    }
}
