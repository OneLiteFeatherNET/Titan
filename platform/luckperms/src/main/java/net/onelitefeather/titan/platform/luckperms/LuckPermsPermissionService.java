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
package net.onelitefeather.titan.platform.luckperms;

import io.avaje.inject.PostConstruct;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.UUID;
import me.lucko.luckperms.minestom.loader.MinestomLoader;
import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.extensions.DiscoveredExtension;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extensions.ExtensionManager;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;

/**
 * Resolves player permissions through LuckPerms, started here in the loader's bootstrap so it is
 * ready before the first player connects.
 */
@Singleton
@Named(LuckPermsPermissionService.QUALIFIER)
public final class LuckPermsPermissionService implements PermissionService {

    /** The Avaje qualifier tests use to mock this bean without starting real LuckPerms. */
    public static final String QUALIFIER = "luckperms";

    @PostConstruct
    void start() {
        LuckPermsExtensionCheck.ensureNotLoadedTwice(loadedExtensionNames());
        MinestomLoader.get().load().registerShutdownHook().start();
    }

    private static List<String> loadedExtensionNames() {
        ExtensionManager extensionManager = ExtensionBootstrap.getExtensionManager();
        if (extensionManager == null) {
            return List.of();
        }
        return extensionManager.getExtensions().stream().map(Extension::getOrigin).map(DiscoveredExtension::getName).toList();
    }

    @Override
    public PermissionResult check(UUID playerId, String permission) {
        User user = LuckPermsProvider.get().getUserManager().getUser(playerId);
        if (user == null) {
            return PermissionResult.NOT_SET;
        }
        return LuckPermsResults.from(user.getCachedData().getPermissionData(queryOptions(playerId)).checkPermission(permission));
    }

    private static QueryOptions queryOptions(UUID playerId) {
        Player onlinePlayer = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        if (onlinePlayer == null) {
            return LuckPermsProvider.get().getContextManager().getStaticQueryOptions();
        }
        return LuckPermsProvider.get().getContextManager().getQueryOptions(onlinePlayer);
    }

    @Override
    public String name() {
        return QUALIFIER;
    }
}
