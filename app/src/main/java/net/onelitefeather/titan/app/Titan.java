package net.onelitefeather.titan.app;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.agones.AgonesAPI;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.app.listener.DeathListener;
import net.onelitefeather.titan.app.listener.ElytraBoostListener;
import net.onelitefeather.titan.app.listener.ElytraStartFlyingListener;
import net.onelitefeather.titan.app.listener.ElytraStopFlyingListener;
import net.onelitefeather.titan.app.listener.NavigationListener;
import net.onelitefeather.titan.app.listener.PlayerConfigurationListener;
import net.onelitefeather.titan.app.listener.PlayerSpawnListener;
import net.onelitefeather.titan.app.listener.RespawnListener;
import net.onelitefeather.titan.app.listener.SitDisconnectListener;
import net.onelitefeather.titan.app.listener.SitDismountListener;
import net.onelitefeather.titan.app.listener.SitLeavePacketListener;
import net.onelitefeather.titan.app.listener.SitListener;
import net.onelitefeather.titan.app.listener.TickleListener;
import net.onelitefeather.titan.common.config.AppConfigProvider;
import net.onelitefeather.titan.common.deliver.DummyDeliver;
import net.onelitefeather.titan.common.event.EntityDismountEvent;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.utils.Cancelable;

import java.nio.file.Path;
import java.time.temporal.ChronoUnit;

public final class Titan {

    private final Path path;
    private final EventNode<Event> eventNode = EventNode.all("titan");
    private final Deliver deliver = new DummyDeliver();
    private final MapProvider mapProvider;
    private final AppConfigProvider appConfigProvider;
    private final NavigationHelper navigationHelper;

    private Titan() {
        this.path = Path.of("");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.mapProvider = MapProvider.create(this.path, instance);
        this.appConfigProvider = AppConfigProvider.create(this.path);
        this.navigationHelper = NavigationHelper.instance(this.deliver);

        BlockHandlerHelper.registerAll();

        initListeners();
        initCommands();
    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(EndCommand.instance());
    }

    private void initListeners() {

        this.eventNode.addListener(PickupItemEvent.class, Cancelable::cancel);
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

        this.eventNode.addListener(PlayerStartFlyingWithElytraEvent.class, new ElytraStartFlyingListener(this.appConfigProvider.getAppConfig()));
        this.eventNode.addListener(PlayerStartFlyingWithElytraEvent.class, new ElytraStopFlyingListener(this.appConfigProvider.getAppConfig()));
        this.eventNode.addListener(PlayerUseItemEvent.class, new ElytraBoostListener(this.appConfigProvider.getAppConfig()));

        this.eventNode.addListener(PlayerRespawnEvent.class, new RespawnListener(this.navigationHelper));

        this.eventNode.addListener(AsyncPlayerConfigurationEvent.class, new PlayerConfigurationListener(this.mapProvider));
        this.eventNode.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(this.appConfigProvider.getAppConfig(), this.mapProvider.getActiveLobby(), this.navigationHelper));

        this.eventNode.addListener(ServerTickMonitorEvent.class, event -> {
            AgonesAPI.instance().alive();
        });
        MinecraftServer.getSchedulerManager().buildTask(this::onUpdateAgones).repeat(this.appConfigProvider.getAppConfig().updateRateAgones(), ChronoUnit.MILLIS).schedule();

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    private void onUpdateAgones() {
        int onlinePlayerSize = MinecraftServer.getConnectionManager().getOnlinePlayers().size();
        if (onlinePlayerSize > 0) {
            AgonesAPI.instance().allocate();
        } else {
            AgonesAPI.instance().ready();
        }
    }

    public static Titan instance() {
        return new Titan();
    }
}
