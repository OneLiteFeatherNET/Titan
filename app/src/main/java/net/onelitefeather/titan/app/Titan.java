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
import net.onelitefeather.titan.common.observability.TitanObservability;
import net.onelitefeather.titan.common.utils.Cancelable;

import java.nio.file.Path;

import static net.onelitefeather.titan.common.observability.TitanObservability.guard;

public final class Titan {

    private final Path path;
    private final EventNode<Event> eventNode = EventNode.all("titan");
    private final Deliver deliver = DeliverProvider.create();
    private final MapProvider mapProvider;
    private final AppConfigProvider appConfigProvider;
    private final NavigationHelper navigationHelper;

    public Titan() {
        MinecraftServer.getConnectionManager().setPlayerProvider(TitanPlayer::new);
        this.path = Path.of("");
        BlockHandlerHelper.registerAll();
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.mapProvider = MapProvider.create(this.path, instance);
        this.appConfigProvider = AppConfigProvider.create(this.path);
        this.navigationHelper = NavigationHelper.instance(this.deliver);
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

    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new EndCommand());
        MinecraftServer.getCommandManager().register(new StopCommand());
    }

    /**
     * Registers every listener through {@link TitanObservability#guard}, so a listener that throws
     * leaves behind which player the event belonged to before Minestom's exception handler reports
     * it. The wrapper only acts on the failure path; a listener that returns normally is
     * unaffected.
     */
    private void initListeners() {

        this.eventNode.addListener(PickupItemEvent.class, guard(Cancelable::cancel));
        this.eventNode.addListener(InventoryPreClickEvent.class, guard(Cancelable::cancel));
        this.eventNode.addListener(PlayerBlockBreakEvent.class, guard(Cancelable::cancel));
        this.eventNode.addListener(PlayerBlockPlaceEvent.class, guard(Cancelable::cancel));
        this.eventNode.addListener(PlayerSwapItemEvent.class, guard(Cancelable::cancel));
        this.eventNode.addListener(ItemDropEvent.class, guard(Cancelable::cancel));

        this.eventNode.addListener(PlayerDeathEvent.class, guard(new DeathListener()));
        this.eventNode.addListener(EntityAttackEvent.class, guard(new TickleListener(this.appConfigProvider.getAppConfig())));

        this.eventNode.addListener(PlayerBlockInteractEvent.class, guard(new SitListener(this.appConfigProvider.getAppConfig())));
        this.eventNode.addListener(PlayerPacketEvent.class, guard(new SitLeavePacketListener()));
        this.eventNode.addListener(EntityDismountEvent.class, guard(new SitDismountListener()));
        this.eventNode.addListener(PlayerDisconnectEvent.class, guard(new SitDisconnectListener()));

        this.eventNode.addListener(PlayerUseItemEvent.class, guard(new NavigationListener(this.navigationHelper)));

        this.eventNode.addListener(PlayerStartFlyingWithElytraEvent.class, guard(new ElytraStartFlyingListener()));
        this.eventNode.addListener(PlayerStopFlyingWithElytraEvent.class, guard(new ElytraStopFlyingListener()));
        this.eventNode.addListener(PlayerUseItemEvent.class, guard(new ElytraBoostListener(this.appConfigProvider.getAppConfig())));

        this.eventNode.addListener(PlayerRespawnEvent.class, guard(new RespawnListener(this.navigationHelper)));
        this.eventNode.addListener(PlayerMoveEvent.class, guard(new PlayerMoveListener(this.appConfigProvider.getAppConfig(), this.mapProvider.getActiveLobby())));

        this.eventNode.addListener(AsyncPlayerConfigurationEvent.class, guard(new PlayerConfigurationListener(this.mapProvider)));
        this.eventNode.addListener(PlayerSpawnEvent.class, guard(new PlayerSpawnListener(
                this.appConfigProvider.getAppConfig(), this.mapProvider.getActiveLobby(), this.navigationHelper)));

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    public static Titan instance() {
        return new Titan();
    }
}
