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
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.butterfly.minestom.Butterfly;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.commands.SeasonCommand;
import net.onelitefeather.titan.app.commands.StopCommand;
import net.onelitefeather.titan.app.feature.LuckPermsFeatureAudience;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.app.listener.*;
import net.onelitefeather.titan.app.player.TitanPlayer;
import net.onelitefeather.titan.common.config.AppConfigProvider;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.SeasonWindowActivationStrategy;
import net.onelitefeather.titan.common.deliver.DeliverProvider;
import net.onelitefeather.titan.common.event.EntityDismountEvent;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.utils.Cancelable;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;

public final class Titan {

    private final Path path;
    private final EventNode<Event> eventNode = EventNode.all("titan");
    private final Deliver deliver = DeliverProvider.create();
    private final MapProvider mapProvider;
    private final AppConfigProvider appConfigProvider;
    private final NavigationHelper navigationHelper;
    private final FeatureGate featureGate;

    public Titan() {
        this(Clock.system(SeasonWindowActivationStrategy.DEFAULT_ZONE), SeasonWindowActivationStrategy.DEFAULT_ZONE);
    }

    /**
     * Creates the lobby with an explicit time source, so seasons and release windows can be tested
     * without waiting for real time (NFR-007).
     *
     * @param clock the time source release windows are evaluated against
     * @param zone  the zone seasons are planned in
     */
    public Titan(Clock clock, ZoneId zone) {
        MinecraftServer.getConnectionManager().setPlayerProvider(TitanPlayer::new);
        this.path = Path.of("");
        BlockHandlerHelper.registerAll();
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.mapProvider = MapProvider.create(this.path, instance);
        this.appConfigProvider = AppConfigProvider.create(this.path);
        this.featureGate = FeatureGate.create(LuckPermsFeatureAudience.create(), clock, zone);
        this.navigationHelper = NavigationHelper.instance(this.deliver, this.featureGate);
    }

    public void initialize() {
        initListeners();
        initCommands();
        Butterfly butterfly = Butterfly.create();
        butterfly.load();
        // Stages live in a flag file that is reloaded in the background, so a stage change is a
        // difference between two observations rather than an event. Walk the features once a
        // second so a transition is logged even while nobody is online (US-3.09).
        MinecraftServer.getSchedulerManager().scheduleTask(
                this.featureGate::pollStageTransitions, TaskSchedule.seconds(1), TaskSchedule.seconds(1));
        MinecraftServer.getSchedulerManager().buildShutdownTask(this::terminate);
        MinecraftServer.getSchedulerManager().buildShutdownTask(butterfly::terminate);
    }

    public void terminate() {

    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new EndCommand());
        MinecraftServer.getCommandManager().register(new StopCommand());
        MinecraftServer.getCommandManager().register(new SeasonCommand(this.featureGate));
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

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    public static Titan instance() {
        return new Titan();
    }
}
