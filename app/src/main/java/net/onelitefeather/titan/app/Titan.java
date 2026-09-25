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
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.butterfly.minestom.Butterfly;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.commands.StopCommand;
import net.onelitefeather.titan.app.feature.elytra.ElytraModule;
import net.onelitefeather.titan.app.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.app.feature.protection.ProtectionModule;
import net.onelitefeather.titan.app.feature.respawn.RespawnModule;
import net.onelitefeather.titan.app.feature.sit.SitModule;
import net.onelitefeather.titan.app.feature.spawn.SpawnModule;
import net.onelitefeather.titan.app.feature.tickle.TickleModule;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.player.TitanPlayer;
import net.onelitefeather.titan.common.config.ConfigStore;
import net.onelitefeather.titan.common.deliver.DeliverProvider;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.onelitefeather.titan.common.feature.TogglzFeatureFlags;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;
import net.onelitefeather.titan.common.map.MapProvider;

import java.nio.file.Path;

/**
 * The lobby's composition root.
 *
 * <p>Builds the shared dependencies every feature module is wired from - the {@link TitanPlayer}
 * provider, the lobby {@link InstanceContainer} and its {@link MapProvider}, the {@link Deliver}
 * used to send a player elsewhere, and the {@link ConfigStore} backing {@code app.json} - and hands
 * them to a {@link ModuleRegistry} of the seven lobby feature modules, in the fixed order
 * protection,
 * spawn, respawn, navigator, sit, tickle, elytra. What is left outside the module platform is
 * exactly what was never a per-player listener to begin with: the {@code stop}/{@code end} commands
 * and the Butterfly extension bridge.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/design.md} and task 6.8. Before this change,
 * {@code Titan#initListeners()} hand-wired nineteen listeners directly onto a shared event node;
 * every one of those now lives inside its own
 * {@link net.onelitefeather.titan.app.module.LobbyModule}
 * and that method is gone.
 */
public final class Titan {

    private static final String APP_FILE_NAME = "app.json";
    private static final String TITAN_NODE_NAME = "titan";

    private final EventNode<Event> titanNode = EventNode.all(TITAN_NODE_NAME);
    private final ModuleRegistry moduleRegistry;

    public Titan() {
        MinecraftServer.getConnectionManager().setPlayerProvider(TitanPlayer::new);
        BlockHandlerHelper.registerAll();

        Path path = Path.of("");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        MapProvider mapProvider = MapProvider.create(path, instance);
        Deliver deliver = DeliverProvider.create();
        ConfigStore configStore = ConfigStore.open(path.resolve(APP_FILE_NAME));

        // Shared platform state, handed to both the registry (which every module can reach
        // through ModuleContext) and, where a module needs the whole thing rather than the
        // narrow add-only view ModuleContext exposes, directly into that module's constructor -
        // see NavigatorModule, which reads every module's entries back at inventory-open time.
        NavigatorEntries navigatorEntries = new NavigatorEntries();
        ItemRegistry itemRegistry = new ItemRegistry(this.titanNode);
        FeatureFlags featureFlags = new TogglzFeatureFlags();

        MinecraftServer.getGlobalEventHandler().addChild(this.titanNode);

        this.moduleRegistry = ModuleRegistry.builder().parent(this.titanNode).config(configStore).navigator(navigatorEntries).items(itemRegistry).modules(
                new ProtectionModule(), new SpawnModule(mapProvider.getInstance(), () -> mapProvider.getActiveLobby().spawn()), new RespawnModule(), new NavigatorModule(deliver, navigatorEntries, featureFlags), new SitModule(), new TickleModule(), new ElytraModule()).build();
    }

    /**
     * Starts every lobby feature module, registers the platform commands and loads Butterfly, then
     * schedules a shutdown task that disables the modules. Because
     * {@link net.minestom.server.timer.SchedulerManager} runs shutdown tasks in the order they were
     * registered, this task runs before Butterfly's own shutdown task, so module cleanup still runs
     * while Butterfly is loaded.
     *
     * @throws net.onelitefeather.titan.common.config.ConfigException       if a module's
     *                                                                      {@code app.json}
     *                                                                      section contains an
     *                                                                      invalid
     *                                                                      value
     * @throws net.onelitefeather.titan.app.module.ModuleLifecycleException if a module fails to
     *                                                                      enable
     */
    public void initialize() {
        this.moduleRegistry.enableAll();
        initCommands();

        Butterfly butterfly = Butterfly.create();
        butterfly.load();

        MinecraftServer.getSchedulerManager().buildShutdownTask(this::terminate);
        MinecraftServer.getSchedulerManager().buildShutdownTask(butterfly::terminate);
    }

    public void terminate() {
        this.moduleRegistry.disableAll();
    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new EndCommand());
        MinecraftServer.getCommandManager().register(new StopCommand());
    }

    public static Titan instance() {
        return new Titan();
    }
}
