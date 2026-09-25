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

import io.avaje.config.Configuration;
import io.avaje.inject.BeanScope;
import io.avaje.inject.BeanScopeBuilder;
import io.avaje.inject.spi.GenericType;
import java.util.List;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.butterfly.minestom.Butterfly;
import net.onelitefeather.titan.app.bootstrap.ConfigurationLoader;
import net.onelitefeather.titan.app.bootstrap.ConfigurationPropertyPlugin;
import net.onelitefeather.titan.app.bootstrap.ConfigurationStartupLog;
import net.onelitefeather.titan.app.bootstrap.ModuleStartupLog;
import net.onelitefeather.titan.app.bootstrap.PlatformBeans;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.commands.StopCommand;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.player.TitanPlayer;
import net.onelitefeather.titan.common.config.ConfigSections;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;

/**
 * The lobby's composition root.
 *
 * <p>Builds an Avaje Inject {@link BeanScope} - which discovers every lobby feature module as a
 * {@code @Singleton} bean and every platform service {@code app/.../bootstrap/PlatformBeans}
 * provides - then builds a {@link ModuleRegistry} from it: the modules, sorted by
 * {@code @Priority} via {@link BeanScope#listByPriority(Class)}, plus the platform beans the
 * registry itself needs. See {@code openspec/changes/avaje-dependency-injection/design.md},
 * decisions 2 and 5, for why the registry is built here rather than as a bean of its own. What is
 * left outside the module platform is exactly what was never a per-player listener to begin with:
 * the {@code stop}/{@code end} commands and the Butterfly extension bridge.
 *
 * <p>See also {@code openspec/changes/lobby-feature-modules/design.md} and task 6.8: before that
 * change, {@code Titan#initListeners()} hand-wired nineteen listeners directly onto a shared event
 * node; every one of those now lives inside its own
 * {@link net.onelitefeather.titan.app.module.LobbyModule}, and this class holds no list of them at
 * all any more - {@link BeanScope#listByPriority(Class)} discovers them.
 */
public final class Titan {

    private final BeanScope beanScope;
    private final List<LobbyModule> modules;
    private final ModuleRegistry moduleRegistry;

    /**
     * @throws net.onelitefeather.titan.common.config.ConfigException if {@code application.yaml}
     *                                                                (or a profile/external file
     *                                                                it pulls in) cannot be
     *                                                                parsed; see the {@code
     *                                                                 lobby-module-config} spec
     *                                                                scenario "Syntaktisch kaputte
     *                                                                Datei" and {@link
     *                                                                net.onelitefeather.titan.common.config.ConfigurationFactory#load()}
     */
    public Titan() {
        MinecraftServer.getConnectionManager().setPlayerProvider(TitanPlayer::new);
        BlockHandlerHelper.registerAll();

        // Loaded exactly once, here, before the BeanScope is built - never via avaje-inject's
        // default property plugin, which would touch the static io.avaje.config.Config facade and
        // load application.yaml a second time (see design.md, decision 1, and
        // ConfigurationPropertyPlugin's Javadoc for why that turned a broken file into a hang
        // instead of a clean abort). The same instance is handed to the scope both as a supplied
        // bean - so PlatformBeans#configSections resolves it instead of building its own - and as
        // the property plugin's backing source.
        Configuration configuration = new ConfigurationLoader().load();
        ConfigurationStartupLog.activeProfiles(configuration);

        BeanScopeBuilder beanScopeBuilder = BeanScope.builder().bean(Configuration.class, configuration);
        // configPlugin(...) returns void, not the builder (unlike bean(...)), so it cannot be
        // chained into the fluent call above.
        beanScopeBuilder.configPlugin(new ConfigurationPropertyPlugin(configuration));
        this.beanScope = beanScopeBuilder.build();
        this.modules = this.beanScope.listByPriority(LobbyModule.class);

        EventNode<Event> titanNode = this.beanScope.get(new GenericType<EventNode<Event>>() {
        }.type(), PlatformBeans.TITAN_NODE_NAME);
        ConfigSections configSections = this.beanScope.get(ConfigSections.class);
        NavigatorEntries navigatorEntries = this.beanScope.get(NavigatorEntries.class);
        ItemRegistry itemRegistry = this.beanScope.get(ItemRegistry.class);
        FeatureFlags featureFlags = this.beanScope.get(FeatureFlags.class);

        this.moduleRegistry = ModuleRegistry.builder().parent(titanNode).config(configSections).navigator(navigatorEntries).items(itemRegistry).featureFlags(featureFlags).modules(this.modules).build();
    }

    /**
     * Starts every lobby feature module, logs the order they were enabled in, registers the
     * platform commands and loads Butterfly, then schedules shutdown tasks in this order (Minestom
     * runs {@link net.minestom.server.timer.SchedulerManager} shutdown tasks FIFO, in the order
     * they were registered): disabling every module, then Butterfly, then closing the
     * {@link BeanScope}.
     *
     * @throws net.onelitefeather.titan.common.config.ConfigException       if a module's
     *                                                                      configuration section
     *                                                                      contains an invalid
     *                                                                      value
     * @throws net.onelitefeather.titan.app.module.ModuleLifecycleException if a module fails to
     *                                                                      enable
     */
    public void initialize() {
        this.moduleRegistry.enableAll();
        ModuleStartupLog.enabledInOrder(this.modules.stream().map(LobbyModule::id).toList());
        initCommands();

        Butterfly butterfly = Butterfly.create();
        butterfly.load();

        MinecraftServer.getSchedulerManager().buildShutdownTask(this::terminate);
        MinecraftServer.getSchedulerManager().buildShutdownTask(butterfly::terminate);
        MinecraftServer.getSchedulerManager().buildShutdownTask(this.beanScope::close);
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
