/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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

public final class TitanPlayer extends Player implements PermissionChecker {

    private final @NotNull Pointers pointers = TitanPlayer.super.pointers().toBuilder().withDynamic(PermissionChecker.POINTER, this::getPermissionChecker).build();

    private @NotNull PermissionChecker getPermissionChecker() {
        return this;
    }

    public TitanPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    @Override
    public Pointers pointers() {
        return this.pointers;
    }


    @Override
    public @NotNull TriState value(@NotNull String permission) {
        QueryOptions queryOptions = LuckPermsProvider.get().getContextManager().getQueryOptions(this);
        User user = LuckPermsProvider.get().getUserManager().getUser(this.getUuid());
        if (user == null) {
            return TriState.FALSE;
        }
        return CompatibilityUtil.convertTriState(user.getCachedData().getPermissionData(queryOptions).checkPermission(permission));
    }
}
