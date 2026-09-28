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
package net.onelitefeather.titan.runtime;

import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.GenericType;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.runtime.bootstrap.ConfigurationStartupLog;
import net.onelitefeather.titan.runtime.bootstrap.FeatureStartupLog;
import net.onelitefeather.titan.runtime.bootstrap.PermissionStartupLog;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.runtime.player.TitanPlayer;
import net.onelitefeather.titan.runtime.variant.VariantStartupCheck;
import net.onelitefeather.titan.common.helper.BlockHandlerHelper;

/**
 * The lobby's composition root: builds an Avaje Inject {@link BeanScope}, which discovers every
 * lobby feature and platform service as a plain {@code @Singleton} bean. A feature's own
 * {@code @PostConstruct}/{@code @PreDestroy} methods are its whole lifecycle.
 */
public final class Titan {

    private final BeanScope beanScope;

    /**
     * @throws ExceptionInInitializerError if {@code application.yaml} cannot be parsed
     * @throws RuntimeException            if a feature's {@code @PostConstruct} throws while the
     *                                     {@link BeanScope} is being built
     * @throws IllegalStateException       if the running variant expected a column that did not
     *                                     load (see {@link VariantStartupCheck})
     */
    public Titan() {
        BlockHandlerHelper.registerAll();

        // First touch of the static io.avaje.config.Config facade, so a broken application.yaml
        // surfaces here as ExceptionInInitializerError.
        ConfigurationStartupLog.activeProfiles();

        // Runs every feature's @PostConstruct, attaching it to the titan event node before any
        // player can connect.
        this.beanScope = BeanScope.builder().build();

        VariantStartupCheck.verify(Titan.class.getClassLoader());

        // Players can only connect once bootstrap.start() runs, well after this point, so the
        // provider can safely use the PermissionService resolved from the scope.
        PermissionService permissionService = this.beanScope.get(PermissionService.class);
        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) -> new TitanPlayer(connection, gameProfile, permissionService));
        PermissionStartupLog.activeService(permissionService);

        EventNode<Event> titan = this.beanScope.get(new GenericType<EventNode<Event>>() {
        }.type(), FeatureNode.TITAN_NODE);
        FeatureStartupLog.startedInEventOrder(titan);
    }

    /**
     * Schedules the {@link BeanScope} to close (running every feature's {@code @PreDestroy}) on
     * shutdown.
     */
    public void initialize() {
        MinecraftServer.getSchedulerManager().buildShutdownTask(this.beanScope::close);
    }

    public static Titan instance() {
        return new Titan();
    }
}
