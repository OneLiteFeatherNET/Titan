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

import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.GenericType;
import java.util.List;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.butterfly.minestom.Butterfly;
import net.onelitefeather.titan.app.bootstrap.ConfigurationStartupLog;
import net.onelitefeather.titan.app.bootstrap.ModuleStartupLog;
import net.onelitefeather.titan.app.bootstrap.PlatformBeans;
import net.onelitefeather.titan.app.bootstrap.reload.ConfigReloadBootstrap;
import net.onelitefeather.titan.app.bootstrap.reload.ConfigReloader;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.commands.ReloadCommand;
import net.onelitefeather.titan.app.commands.StopCommand;
import net.onelitefeather.titan.app.i18n.TitanTranslations;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.player.TitanPlayer;
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
     * @throws ExceptionInInitializerError if {@code application.yaml} (or a profile/external file
     *                                     it pulls in) cannot be parsed; the {@code
     *                                     io.avaje.config.Config} facade's own static initializer
     *                                     throws this on its first touch, wrapping the underlying
     *                                     parser failure (file and line/column) as its cause; see
     *                                     the {@code lobby-module-config} spec scenario
     *                                     "Syntaktisch kaputte Datei".
     */
    public Titan() {
        MinecraftServer.getConnectionManager().setPlayerProvider(TitanPlayer::new);
        BlockHandlerHelper.registerAll();

        // ConfigurationStartupLog#activeProfiles() is the first thing this constructor touches
        // the static io.avaje.config.Config facade for - and the first touch of Config at all in
        // this JVM - deliberately, at a known place in the start sequence (built-in first: no
        // factory of our own wraps this touch; see design.md, decision 1). A broken
        // application.yaml surfaces here as ExceptionInInitializerError, whose cause chain already
        // names the file and the line/column. Once this returns, the facade is the single,
        // already-built Configuration instance for the rest of the process - Avaje Inject's own
        // default config property plugin reading the same facade while the scope below is built is
        // then just a second read of that instance, not a second first touch.
        ConfigurationStartupLog.activeProfiles();

        this.beanScope = BeanScope.builder().build();
        this.modules = this.beanScope.listByPriority(LobbyModule.class);

        EventNode<Event> titanNode = this.beanScope.get(new GenericType<EventNode<Event>>() {
        }.type(), PlatformBeans.TITAN_NODE_NAME);
        NavigatorEntries navigatorEntries = this.beanScope.get(NavigatorEntries.class);
        ItemRegistry itemRegistry = this.beanScope.get(ItemRegistry.class);
        FeatureFlags featureFlags = this.beanScope.get(FeatureFlags.class);

        this.moduleRegistry = ModuleRegistry.builder().parent(titanNode).navigator(navigatorEntries).items(itemRegistry).featureFlags(featureFlags).modules(this.modules).build();
    }

    /**
     * Starts every lobby feature module, logs the order they were enabled in, registers the
     * platform commands and loads Butterfly, then schedules shutdown tasks in this order (Minestom
     * runs {@link net.minestom.server.timer.SchedulerManager} shutdown tasks FIFO, in the order
     * they were registered): disabling every module, then Butterfly, then closing the
     * {@link BeanScope}.
     *
     * @throws IllegalArgumentException                                     if a module's
     *                                                                      configuration section
     *                                                                      contains an invalid
     *                                                                      value
     * @throws net.onelitefeather.titan.app.module.ModuleLifecycleException if a module fails to
     *                                                                      enable
     */
    public void initialize() {
        this.moduleRegistry.enableAll();
        ModuleStartupLog.enabledInOrder(this.modules.stream().map(LobbyModule::id).toList());
        TitanTranslations.register(GlobalTranslator.translator());
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

        ConfigReloader configReloader = ConfigReloadBootstrap.install(this.moduleRegistry);
        MinecraftServer.getCommandManager().register(new ReloadCommand(configReloader::reload));
    }

    public static Titan instance() {
        return new Titan();
    }
}
