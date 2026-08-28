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

import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.onelitefeather.titan.common.resourcepack.ResourcePackService;

import java.util.function.Consumer;

/**
 * Pushes the configured resource packs while the player is still in the configuration phase, so
 * the client downloads them before it reaches the lobby.
 *
 * <p>This runs on the configuration thread that {@code ConnectionManager.doConfiguration()} will
 * park on the pack future a moment later; the service arms its own timeout here so that thread
 * cannot be parked forever.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class ResourcePackConfigurationListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private final ResourcePackService resourcePackService;

    /**
     * Creates the listener.
     *
     * @param resourcePackService the service that decides what to deliver
     */
    public ResourcePackConfigurationListener(ResourcePackService resourcePackService) {
        this.resourcePackService = resourcePackService;
    }

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        this.resourcePackService.onConfiguration(event.getPlayer());
    }
}
