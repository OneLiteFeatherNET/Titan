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

import net.minestom.server.event.player.PlayerMoveEvent;
import net.onelitefeather.titan.common.portal.PortalService;

import java.util.function.Consumer;

/**
 * Feeds player movement to {@link PortalService}.
 *
 * <p>Deliberately empty of logic. This runs on every movement packet of every player, and every
 * decision it could make here - is this a portal, did they just enter it, may they use it - is one
 * the service already makes, in that order and with a cheap first step.
 *
 * <p>It reads the event's new position rather than the player's current one: the player has not
 * been moved yet when the event fires, so the current position is where they came from.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class PortalListener implements Consumer<PlayerMoveEvent> {

    private final PortalService portalService;

    /**
     * Creates the listener.
     *
     * @param portalService the service deciding what a movement into a portal does
     */
    public PortalListener(PortalService portalService) {
        this.portalService = portalService;
    }

    @Override
    public void accept(PlayerMoveEvent event) {
        this.portalService.handleMove(event.getPlayer(), event.getNewPosition());
    }
}
