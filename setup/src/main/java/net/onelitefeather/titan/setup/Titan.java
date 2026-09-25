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
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;
import net.onelitefeather.titan.common.map.MapEntry;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.common.utils.Cancelable;
import net.onelitefeather.titan.setup.commands.SetupCommand;
import net.onelitefeather.titan.setup.config.SetupSpawnConfig;
import net.onelitefeather.titan.setup.listener.PlayerConfigurationListener;
import net.onelitefeather.titan.setup.listener.PlayerSpawnListener;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class Titan {

    private final Path path;
    private final EventNode<Event> eventNode = EventNode.all("titan");
    private final MapProvider mapProvider;
    private final int simulationDistance;

    /**
     * @throws ExceptionInInitializerError if {@code application.yaml} (or a profile/external file
     *                                     it pulls in) cannot be parsed; the static
     *                                     {@code io.avaje.config.Config} facade throws this from
     *                                     its own static initializer on first touch, wrapping the
     *                                     underlying parser failure (file and line/column) as its
     *                                     cause. {@link
     *                                     net.onelitefeather.titan.setup.TitanLauncher#main}
     *                                     aborts cleanly when this propagates out of
     *                                     {@link #instance()}.
     */
    private Titan() {
        this.path = Path.of("");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        this.mapProvider = MapProvider.create(this.path, instance, Titan::defaultFilter);
        // SetupSpawnConfig#read() is the first touch of the static io.avaje.config.Config facade
        // in this process - deliberately, at a known, early place (built-in first: no factory of
        // our own wraps this touch; see design.md, decision 1). A broken application.yaml surfaces
        // here as ExceptionInInitializerError, whose cause chain already names the file and the
        // line/column.
        this.simulationDistance = SetupSpawnConfig.read().simulationDistance();
        BlockHandlerHelper.registerAll();

        initCommands();
        initListeners();
    }

    private void initListeners() {
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, new PlayerConfigurationListener(this.mapProvider));
        eventNode.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(this.simulationDistance, this.mapProvider));
        eventNode.addListener(InventoryPreClickEvent.class, Cancelable::cancel);
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new SetupCommand(this.mapProvider));
    }

    private static List<MapEntry> defaultFilter(Stream<Path> pathStream) {
        return pathStream.map(MapEntry::new).toList();
    }

    public static Titan instance() {
        return new Titan();
    }
}
