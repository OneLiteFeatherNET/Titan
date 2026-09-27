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
import net.minestom.server.MinecraftServer;
import net.onelitefeather.butterfly.minestom.Butterfly;
import net.onelitefeather.titan.app.bootstrap.ConfigurationStartupLog;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.commands.StopCommand;
import net.onelitefeather.titan.app.player.TitanPlayer;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;

/**
 * The lobby's composition root.
 *
 * <p>Builds an Avaje Inject {@link BeanScope} - which discovers every lobby feature as a plain
 * {@code @Singleton} bean and every platform service {@code app/.../bootstrap/PlatformBeans}
 * provides. There is no separate module registry any more: a feature's own
 * {@code @PostConstruct}/{@code @PreDestroy} methods, run by the {@code BeanScope} itself, are its
 * whole lifecycle (see {@code openspec/changes/dissolve-module-platform/design.md}, decisions 1 and
 * 4). What is left outside the feature beans is exactly what was never a per-player listener to
 * begin with: the {@code stop}/{@code end} commands and the Butterfly extension bridge.
 *
 * <p>See also {@code openspec/changes/lobby-feature-modules/design.md} and task 6.8: before that
 * change, {@code Titan#initListeners()} hand-wired nineteen listeners directly onto a shared event
 * node; every one of those now lives inside its own feature bean, discovered by
 * {@link BeanScope#builder()} rather than named here.
 */
public final class Titan {

    private final BeanScope beanScope;

    /**
     * @throws ExceptionInInitializerError if {@code application.yaml} (or a profile/external file
     *                                     it pulls in) cannot be parsed; the {@code
     *                                     io.avaje.config.Config} facade's own static initializer
     *                                     throws this on its first touch, wrapping the underlying
     *                                     parser failure (file and line/column) as its cause; see
     *                                     the {@code lobby-module-config} spec scenario
     *                                     "Syntaktisch kaputte Datei".
     * @throws RuntimeException            if a feature's {@code @PostConstruct} throws while the
     *                                     {@link BeanScope} is being built; the lobby does not
     *                                     start, and the message names the failing feature's bean
     *                                     class (see the {@code lobby-modules} spec scenario
     *                                     "Fehler beim Start eines Features").
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

        // Building the scope runs every bean's @PostConstruct, including every feature's own
        // start() - so every feature is already attached to the titan event node once this
        // constructor returns, before any player can connect (lobby-modules spec, "Features
        // starten vor dem ersten Spieler").
        this.beanScope = BeanScope.builder().build();
    }

    /**
     * Registers the platform commands and loads Butterfly, then schedules shutdown tasks in this
     * order (Minestom runs {@link net.minestom.server.timer.SchedulerManager} shutdown tasks FIFO,
     * in the order they were registered): closing the {@link BeanScope} - which runs every
     * feature's {@code @PreDestroy}, detaching its event node first (lobby-modules spec, "Features
     * trennen sich beim Herunterfahren zuerst von Events") - then Butterfly. Feature shutdown ran
     * before Butterfly before this change too; only the mechanism changed.
     */
    public void initialize() {
        initCommands();

        Butterfly butterfly = Butterfly.create();
        butterfly.load();

        MinecraftServer.getSchedulerManager().buildShutdownTask(this.beanScope::close);
        MinecraftServer.getSchedulerManager().buildShutdownTask(butterfly::terminate);
    }

    private void initCommands() {
        MinecraftServer.getCommandManager().register(new EndCommand());
        MinecraftServer.getCommandManager().register(new StopCommand());
    }

    public static Titan instance() {
        return new Titan();
    }
}
