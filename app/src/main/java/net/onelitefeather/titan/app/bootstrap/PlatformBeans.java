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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.inject.Named;
import java.nio.file.Path;
import java.time.Clock;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.common.deliver.DeliverProvider;
import net.onelitefeather.titan.common.feature.ConfigFeatureFlags;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;

/**
 * Wires the platform services every lobby feature module is built from as Avaje Inject beans, so
 * a module asks for one through its constructor instead of
 * {@link net.onelitefeather.titan.app.Titan} handing it out by hand.
 *
 * <p>{@code common} stays free of Avaje annotations; this factory turns its library types into
 * beans for {@code app}.
 */
@Factory
public final class PlatformBeans {

    /** Also satisfies a bean lookup for the narrower {@link Instance} type. */
    @Bean
    public InstanceContainer instanceContainer() {
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        return instance;
    }

    /** Loads map data from {@code worlds/} relative to the working directory. */
    @Bean
    public MapProvider mapProvider(InstanceContainer instance) {
        return MapProvider.create(Path.of(""), instance);
    }

    @Bean
    public LobbySpawn lobbySpawn(MapProvider mapProvider) {
        return () -> mapProvider.getActiveLobby().spawn();
    }

    /** A no-op outside a CloudNet service. */
    @Bean
    public Deliver deliver() {
        return DeliverProvider.create();
    }

    @Bean
    @Named(FeatureNode.TITAN_NODE)
    public EventNode<Event> titanEventNode() {
        EventNode<Event> node = EventNode.all(FeatureNode.TITAN_NODE);
        MinecraftServer.getGlobalEventHandler().addChild(node);
        return node;
    }

    /**
     * Flags are configuration values under {@code features.*}, defaulted from the classpath
     * {@code application.yaml}.
     */
    @Bean
    public FeatureFlags featureFlags() {
        return ConfigFeatureFlags.fromClasspathDefaults();
    }

    /**
     * Lets a module ask for {@link Clock} instead of {@link System#currentTimeMillis()}, so a test
     * can inject a fixed one.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Scheduler scheduler() {
        return MinecraftServer.getSchedulerManager();
    }
}
