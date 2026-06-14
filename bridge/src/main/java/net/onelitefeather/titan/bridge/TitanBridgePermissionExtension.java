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
package net.onelitefeather.titan.bridge;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomPermissionChecker;
import net.minestom.server.extensions.Extension;
import net.onelitefeather.titan.common.permission.TitanPermissionBridge;

/**
 * Minestom extension that makes the CloudNet bridge resolve permissions through LuckPerms.
 *
 * <p>The bridge injects its permission checker from CloudNet's {@link ServiceRegistry} and ships
 * a default ({@code MinestomDefaultPermissionChecker}) that merely checks {@code
 * player.getPermissionLevel() > 0} and ignores the permission node entirely. This extension
 * registers a LuckPerms-backed checker and marks it as the default, so the bridge picks it up on
 * its next lazy lookup.
 *
 * <p>The checker cannot live in the application: {@link MinestomPermissionChecker} is a bridge
 * class only visible inside the bridge's extension classloader. By declaring a dependency on the
 * {@code CloudNet_Bridge} extension (see {@code extension.json}), this extension loads after the
 * bridge and shares its classloader hierarchy, so it can both implement the interface and reach
 * the registry. The actual permission lookup is delegated back to the application realm through
 * {@link TitanPermissionBridge}, which exchanges only JDK types.
 */
public final class TitanBridgePermissionExtension extends Extension {

    @Override
    public void initialize() {
        MinestomPermissionChecker checker = (player, permission) -> TitanPermissionBridge.hasPermission(player.getUuid(), permission);
        ServiceRegistry.registry().registerProvider(MinestomPermissionChecker.class, "titan-luckperms", checker).markAsDefaultService();
    }

    @Override
    public void terminate() {
    }
}
