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
package net.onelitefeather.titan.app.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.onelitefeather.titan.common.utils.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class ElytraStartFlyingListener implements Consumer<PlayerStartFlyingWithElytraEvent> {

    @Override
    public void accept(@NotNull PlayerStartFlyingWithElytraEvent event) {
        Player player = event.getPlayer();
        player.setItemInOffHand(Items.PLAYER_FIREWORK);
    }
}
