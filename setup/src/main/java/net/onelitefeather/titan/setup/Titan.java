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
package net.onelitefeather.titan.setup;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.titan.common.config.ConfigStore;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;
import net.onelitefeather.titan.common.map.MapEntry;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.utils.Cancelable;
import net.onelitefeather.titan.setup.commands.SetupCommand;
import net.onelitefeather.titan.setup.config.ElytraSectionConfig;
import net.onelitefeather.titan.setup.config.SetupConfigEditor;
import net.onelitefeather.titan.setup.config.SitSectionConfig;
import net.onelitefeather.titan.setup.config.SpawnSectionConfig;
import net.onelitefeather.titan.setup.config.TickleSectionConfig;
import net.onelitefeather.titan.setup.listener.PlayerConfigurationListener;
import net.onelitefeather.titan.setup.listener.PlayerSpawnListener;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class Titan {
    private static final String APP_FILE_NAME = "app.json";

    private final Path path;
    private final EventNode<Event> eventNode = EventNode.all("titan");
    private final MapProvider mapProvider;
    private final ConfigStore configStore;
    private final SetupConfigEditor configEditor;
    private final SpawnSectionConfig spawn;

    private Titan() {
        this.path = Path.of("");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.mapProvider = MapProvider.create(this.path, instance, Titan::defaultFilter);
        this.configStore = ConfigStore.open(this.path.resolve(APP_FILE_NAME));
        // Warm every section this server can edit so a freshly created or migrated app.json
        // ends up with defaults for every section.
        this.spawn = this.configStore.section("spawn", SpawnSectionConfig.class, SpawnSectionConfig.DEFAULTS);
        this.configStore.section("sit", SitSectionConfig.class, SitSectionConfig.DEFAULTS);
        this.configStore.section("tickle", TickleSectionConfig.class, TickleSectionConfig.DEFAULTS);
        this.configStore.section("elytra", ElytraSectionConfig.class, ElytraSectionConfig.DEFAULTS);
        this.configStore.flush();
        this.configEditor = new SetupConfigEditor(this.configStore);
        BlockHandlerHelper.registerAll();

        initCommands();
        initListeners();
    }

    private void initListeners() {
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, new PlayerConfigurationListener(this.mapProvider));
        eventNode.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(this.spawn.simulationDistance(), this.mapProvider));
        eventNode.addListener(InventoryPreClickEvent.class, Cancelable::cancel);
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new SetupCommand(this.configEditor, this.mapProvider));
    }

    private static List<MapEntry> defaultFilter(Stream<Path> pathStream) {
        return pathStream.map(MapEntry::new).toList();
    }

    public static Titan instance() {
        return new Titan();
    }
}
