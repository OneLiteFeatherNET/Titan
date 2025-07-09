/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import net.onelitefeather.titan.common.config.AppConfigProvider;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;
import net.onelitefeather.titan.common.map.MapEntry;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.utils.Cancelable;
import net.onelitefeather.titan.setup.commands.SetupCommand;
import net.onelitefeather.titan.setup.listener.PlayerConfigurationListener;
import net.onelitefeather.titan.setup.listener.PlayerSpawnListener;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class Titan {
    private final Path path;
    private final EventNode<Event> eventNode = EventNode.all("titan");
    private final MapProvider mapProvider;
    private final AppConfigProvider appConfigProvider;

    private Titan() {
        this.path = Path.of("");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.mapProvider = MapProvider.create(this.path, instance, Titan::defaultFilter);
        this.appConfigProvider = AppConfigProvider.create(this.path);
        BlockHandlerHelper.registerAll();

        initCommands();
        initListeners();
    }

    private void initListeners() {
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, new PlayerConfigurationListener(this.mapProvider));
        eventNode.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(this.appConfigProvider.getAppConfig(), this.mapProvider));
        eventNode.addListener(InventoryPreClickEvent.class, Cancelable::cancel);
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new SetupCommand(this.appConfigProvider, this.mapProvider));
    }

    private static List<MapEntry> defaultFilter(Stream<Path> pathStream) {
        return pathStream.map(MapEntry::new).toList();
    }

    public static Titan instance() {
        return new Titan();
    }
}
