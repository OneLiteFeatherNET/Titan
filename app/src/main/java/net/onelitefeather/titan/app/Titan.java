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
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.butterfly.minestom.Butterfly;
import net.onelitefeather.titan.app.bootstrap.ConfigurationStartupLog;
import net.onelitefeather.titan.app.bootstrap.FeatureStartupLog;
import net.onelitefeather.titan.app.commands.EndCommand;
import net.onelitefeather.titan.app.commands.StopCommand;
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.app.player.TitanPlayer;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;

/**
 * The lobby's composition root: builds an Avaje Inject {@link BeanScope}, which discovers every
 * lobby feature and platform service as a plain {@code @Singleton} bean. A feature's own
 * {@code @PostConstruct}/{@code @PreDestroy} methods are its whole lifecycle; only the
 * {@code stop}/{@code end} commands and the Butterfly extension bridge live outside it.
 */
public final class Titan {

    private final BeanScope beanScope;

    /**
     * @throws ExceptionInInitializerError if {@code application.yaml} cannot be parsed
     * @throws RuntimeException            if a feature's {@code @PostConstruct} throws while the
     *                                     {@link BeanScope} is being built
     */
    public Titan() {
        MinecraftServer.getConnectionManager().setPlayerProvider(TitanPlayer::new);
        BlockHandlerHelper.registerAll();

        // First touch of the static io.avaje.config.Config facade, so a broken application.yaml
        // surfaces here as ExceptionInInitializerError.
        ConfigurationStartupLog.activeProfiles();

        // Runs every feature's @PostConstruct, attaching it to the titan event node before any
        // player can connect.
        this.beanScope = BeanScope.builder().build();

        EventNode<Event> titan = this.beanScope.get(new GenericType<EventNode<Event>>() {
        }.type(), FeatureNode.TITAN_NODE);
        FeatureStartupLog.startedInEventOrder(titan);
    }

    /**
     * Registers commands and loads Butterfly, then schedules shutdown in FIFO order: the
     * {@link BeanScope} closes first (running every feature's {@code @PreDestroy}), then Butterfly.
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
