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

import net.kyori.adventure.key.Key;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.helper.SitHelper;

import java.util.List;
import java.util.function.Consumer;

public final class SitListener implements Consumer<PlayerBlockInteractEvent> {

    private final AppConfig appConfig;
    private final List<Key> allowedBlocks;

    public SitListener(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.allowedBlocks = appConfig.allowedSitBlocks();
    }

    @Override
    public void accept(PlayerBlockInteractEvent event) {
        if (this.allowedBlocks.stream().anyMatch(block -> event.getBlock().key().equals(block))) {
            SitHelper.sitPlayer(event.getPlayer(), event.getBlockPosition(), this.appConfig);
        }
    }
}
