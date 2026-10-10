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
package net.onelitefeather.titan.runtime.bootstrap;

import io.avaje.inject.Bean;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.avaje.inject.Factory;
import io.avaje.inject.Profile;
import jakarta.inject.Named;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.module.LobbyWorldChoice;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.common.deliver.DeliverProvider;
import net.onelitefeather.titan.common.deliver.HolderPlayerCounts;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.runtime.feature.ConfigFeatureFlags;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.runtime.deliver.TracedDeliver;

/**
 * Wires the platform services every lobby feature module is built from as Avaje Inject beans, so
 * a module asks for one through its constructor instead of
 * {@link net.onelitefeather.titan.runtime.Titan} handing it out by hand.
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

    /**
     * Loads map data from {@code worlds/} relative to the working directory, in the world a
     * {@link LobbyWorldChoice} names. Looked up through the service loader, not as a bean: a
     * column's bean is built after this one and would always be missing here.
     */
    @Bean
    public MapProvider mapProvider(InstanceContainer instance) {
        return loadMapProvider(Path.of(""), instance, ServiceLoader.load(LobbyWorldChoice.class));
    }

    static MapProvider loadMapProvider(Path base, InstanceContainer instance, Iterable<LobbyWorldChoice> choices) {
        List<LobbyWorldChoice> found = new ArrayList<>();
        choices.forEach(found::add);
        if (found.size() > 1) {
            // Which world wins must not depend on classpath order.
            String names = found.stream().map(choice -> choice.getClass().getName()).collect(Collectors.joining(", "));
            throw new IllegalStateException("More than one LobbyWorldChoice found: " + names);
        }
        Optional<String> worldName = found.stream().findFirst().flatMap(LobbyWorldChoice::worldName);
        return MapProvider.create(base, instance, worldName);
    }

    @Bean
    public LobbySpawn lobbySpawn(MapProvider mapProvider) {
        return () -> mapProvider.getActiveLobby().spawn();
    }

    /**
     * Read per call like {@link #lobbySpawn}; a seasonal world brings the portals of its own
     * map.json.
     */
    @Bean
    public LobbyPortals lobbyPortals(MapProvider mapProvider) {
        return () -> mapProvider.getActiveLobby().portals();
    }

    /** A no-op outside a CloudNet service. Every transfer is traced, whichever feature sends it. */
    @Bean
    public Deliver deliver(Telemetry telemetry) {
        return new TracedDeliver(DeliverProvider.create(), telemetry);
    }

    /**
     * Only as a CloudNet service ({@link BeanProfiles#CLOUDNET}): elsewhere the portal column's
     * fallback answers, so another provider module wins over it without configuration.
     */
    @Bean
    @Profile(BeanProfiles.CLOUDNET)
    public PlayerCounts playerCounts() {
        return new HolderPlayerCounts();
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

    /**
     * The OpenTelemetry Java agent sets the global instance before the first bean is built; with
     * no agent this is a no-op. Beans and features take it from here; only {@code runtime} may
     * touch {@link GlobalOpenTelemetry}.
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    @Bean
    public Telemetry telemetry(OpenTelemetry openTelemetry) {
        return Telemetry.of(openTelemetry);
    }

    @Bean
    public Scheduler scheduler() {
        return MinecraftServer.getSchedulerManager();
    }

    @Bean
    public CommandManager commandManager() {
        return MinecraftServer.getCommandManager();
    }
}
