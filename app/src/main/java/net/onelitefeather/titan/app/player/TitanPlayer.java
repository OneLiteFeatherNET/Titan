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
package net.onelitefeather.titan.app.player;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.util.TriState;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

public class TitanPlayer extends Player implements PermissionChecker {

    private final @NotNull Pointers pointers = TitanPlayer.super.pointers().toBuilder().withDynamic(PermissionChecker.POINTER, this::getPermissionChecker).build();

    private @NotNull PermissionChecker getPermissionChecker() {
        return this;
    }

    public TitanPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    @Override
    public Pointers pointers() {
        return super.pointers();
    }


    @Override
    public @NotNull TriState value(@NotNull String permission) {
        QueryOptions queryOptions = LuckPermsProvider.get().getContextManager().getQueryOptions(this);
        User user = LuckPermsProvider.get().getUserManager().getUser(this.getUuid());
        return CompatibilityUtil.convertTriState(user.getCachedData().getPermissionData(queryOptions).checkPermission(permission));
    }
}
