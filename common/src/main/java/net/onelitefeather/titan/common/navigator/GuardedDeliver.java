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
package net.onelitefeather.titan.common.navigator;

import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Checks the build-server permission again at the moment a switch is requested (US-5.03).
 *
 * <p>The check in the navigator decides what is <em>drawn</em>, and a drawing is not a
 * permission. A click arrives as a packet: it can name a slot the menu never had, it can arrive
 * long after the menu was built, and the permission can have been taken away in between. So the
 * decision is made once more here, on the way out, against the permission the player holds
 * <em>now</em> — and here rather than in the navigator because every path to another server goes
 * through {@link Deliver}, including the ones that never involved a menu.
 *
 * <p>Everything that is not a build server passes through untouched; this decorator guards one
 * destination, it is not a general permission layer.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class GuardedDeliver implements Deliver {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardedDeliver.class);

    private final Deliver delegate;
    private final FeatureAudience audience;
    private final BuildServerAccess access;

    private GuardedDeliver(Deliver delegate, FeatureAudience audience, BuildServerAccess access) {
        this.delegate = delegate;
        this.audience = audience;
        this.access = access;
    }

    /**
     * Wraps a delivery implementation with the build-server check.
     *
     * @param delegate the delivery that performs the switch once it is allowed
     * @param audience the source of permission answers, asked at request time
     * @param access   which destinations are build servers and what they require
     * @return the guarded delivery
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static GuardedDeliver wrap(Deliver delegate, FeatureAudience audience, BuildServerAccess access) {
        return new GuardedDeliver(delegate, audience, access);
    }

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        if (!allows(player.getUuid(), component)) {
            LOGGER.warn("Refused build server switch for {}: player does not hold {}", player.getUuid(), this.access.permission());
            return;
        }
        this.delegate.sendPlayer(player, component);
    }

    /**
     * Decides whether the requested switch may go through.
     *
     * @param playerId  the player requesting the switch
     * @param component the requested destination
     * @return whether the destination is unguarded or the player currently holds the permission
     */
    @Contract(pure = true)
    public boolean allows(UUID playerId, DeliverComponent component) {
        return !this.access.covers(component) || this.audience.hasPermission(playerId, this.access.permission());
    }
}
