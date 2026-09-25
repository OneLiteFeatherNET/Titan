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

import io.avaje.config.Configuration;
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
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.app.module.LobbySpawn;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.common.config.ConfigSections;
import net.onelitefeather.titan.common.deliver.DeliverProvider;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.onelitefeather.titan.common.feature.TogglzFeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;

/**
 * Wires the platform services every lobby feature module is built from as Avaje Inject beans, so a
 * module asks for one through its constructor instead of {@link net.onelitefeather.titan.app.Titan}
 * handing it out by hand.
 *
 * <p>Every bean here mirrors exactly what {@code Titan}'s composition root built directly before
 * this change - see {@code openspec/changes/avaje-dependency-injection/design.md}, decision 3.
 * {@code common} itself stays free of any Avaje annotation or dependency; this factory is what
 * turns its library types into beans for {@code app}.
 *
 * <p>{@code ModuleRegistry} is deliberately <em>not</em> a bean here: the spike behind this change
 * found that {@code BeanScope.listByPriority(LobbyModule.class)} - the only way to get modules
 * sorted by {@code @Priority} - can only be called once {@code BeanScope.builder().build()} has
 * returned, never from inside a {@code @Factory} method while the scope is still being built (see
 * design.md, decision 2). Building the registry from the sorted list is therefore
 * {@code Titan}'s job, after the scope exists.
 */
@Factory
public final class PlatformBeans {

    /**
     * The {@code @Named} qualifier of the shared {@link EventNode} bean {@link #titanEventNode()}
     * registers, so any other class that looks the bean up by name - such as
     * {@link net.onelitefeather.titan.app.Titan} - references this constant instead of duplicating
     * the literal.
     */
    public static final String TITAN_NODE_NAME = "titan";

    /**
     * The collaborator {@link #configuration()} delegates the migrate-then-load sequence to - see
     * that method's Javadoc. Held as a field, rather than the migration and the factory being
     * invoked inline, so the bean method depends on one small collaborator instead of orchestrating
     * global filesystem state itself.
     */
    private final ConfigurationLoader configurationLoader = new ConfigurationLoader();

    /**
     * @return the lobby's single {@link InstanceContainer}, registered with the instance manager -
     *         also satisfies a module constructor asking for the narrower {@link Instance} type
     */
    @Bean
    public InstanceContainer instanceContainer() {
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);
        return instance;
    }

    /**
     * @param instance the lobby instance {@link #instanceContainer()} created
     * @return the provider for the lobby's map data, loaded from {@code worlds/} relative to the
     *         working directory
     */
    @Bean
    public MapProvider mapProvider(InstanceContainer instance) {
        return MapProvider.create(Path.of(""), instance);
    }

    /**
     * @param mapProvider the map provider the current spawn position is read from
     * @return the lobby's current spawn position, read lazily on every
     *         {@link LobbySpawn#position()}
     *         call so a map reload is picked up without rebuilding any module
     */
    @Bean
    public LobbySpawn lobbySpawn(MapProvider mapProvider) {
        return () -> mapProvider.getActiveLobby().spawn();
    }

    /**
     * @return the service a module uses to send a player to another server - a no-op outside a
     *         CloudNet service
     */
    @Bean
    public Deliver deliver() {
        return DeliverProvider.create();
    }

    /**
     * Builds the {@link Configuration} every module's section is ultimately read from, via {@link
     * ConfigurationLoader} - the same collaborator {@link ConfigurationPrintMain} (the child JVM
     * the configuration precedence test drives) calls, so both run the exact same migrate-then-load
     * sequence (DRY) - and logs the active profiles once, at INFO, via {@link
     * ConfigurationStartupLog}.
     *
     * @return the {@link Configuration} every module's section is ultimately read from
     */
    @Bean
    public Configuration configuration() {
        Configuration configuration = configurationLoader.load();
        ConfigurationStartupLog.activeProfiles(configuration);
        return configuration;
    }

    /**
     * @param configuration the {@link Configuration} every module's section is bound from
     * @return the sectioned configuration every module's {@code ModuleContext#config} reads its
     *         own section from
     */
    @Bean
    public ConfigSections configSections(Configuration configuration) {
        return new ConfigSections(configuration);
    }

    /**
     * @return the shared event node every module's own {@code titan/<id>} node attaches under,
     *         itself attached to the global event handler
     */
    @Bean
    @Named(TITAN_NODE_NAME)
    public EventNode<Event> titanEventNode() {
        EventNode<Event> node = EventNode.all(TITAN_NODE_NAME);
        MinecraftServer.getGlobalEventHandler().addChild(node);
        return node;
    }

    /**
     * @param titanNode the shared event node {@link #titanEventNode()} attached to the global
     *                  handler; the registry's dispatch listener attaches to it immediately
     * @return the platform-wide registry of hotbar/equipment items every module registers through
     */
    @Bean
    public ItemRegistry itemRegistry(@Named(TITAN_NODE_NAME) EventNode<Event> titanNode) {
        return new ItemRegistry(titanNode);
    }

    /**
     * @return the platform-wide registry of navigator destinations every module contributes to
     */
    @Bean
    public NavigatorEntries navigatorEntries() {
        return new NavigatorEntries();
    }

    /**
     * @return the source of truth a navigator entry's optional feature gate is checked against,
     *         backed by the static Togglz {@code FeatureContext} and {@code flags.properties}
     */
    @Bean
    public FeatureFlags featureFlags() {
        return new TogglzFeatureFlags();
    }

    /**
     * @return the system clock, so a module that needs "now" (e.g. a cooldown) asks for a
     *         {@link Clock} instead of reading {@link System#currentTimeMillis()} directly and a
     *         test can hand in a fixed one
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
