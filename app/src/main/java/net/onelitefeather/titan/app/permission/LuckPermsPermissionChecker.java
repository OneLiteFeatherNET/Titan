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
package net.onelitefeather.titan.app.permission;

import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomPermissionChecker;
import net.luckperms.api.LuckPermsProvider;
import net.minestom.server.entity.Player;

/**
 * CloudNet bridge {@link MinestomPermissionChecker} backed by LuckPerms. The
 * bridge resolves the checker through CloudNet's injection layer, so installing
 * this binding makes the bridge's permission checks use LuckPerms.
 */
public final class LuckPermsPermissionChecker implements MinestomPermissionChecker {

    @Override
    public boolean hasPermission(Player player, String permission) {
        return LuckPermsProvider.get().getPlayerAdapter(Player.class).getPermissionData(player).checkPermission(permission).asBoolean();
    }

    /**
     * Binds this LuckPerms-backed checker into the external injection layer so the
     * CloudNet bridge uses it. Must run before the bridge is constructed (i.e.
     * before the bridge extension is started).
     */
    public static void install() {
        InjectionLayer<Injector> layer = InjectionLayer.ext();
        layer.install(layer.injector().createBindingBuilder().bind(MinestomPermissionChecker.class).toInstance(new LuckPermsPermissionChecker()));
    }
}
