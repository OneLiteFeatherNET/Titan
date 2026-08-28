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

import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.titan.common.helper.SitHelper;

import java.util.function.Consumer;

public final class SitDisconnectListener implements Consumer<PlayerDisconnectEvent> {
    @Override
    public void accept(PlayerDisconnectEvent event) {
        SitHelper.removePlayer(event.getPlayer());
    }
}
