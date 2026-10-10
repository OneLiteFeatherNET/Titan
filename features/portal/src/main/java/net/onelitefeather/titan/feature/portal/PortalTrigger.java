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
package net.onelitefeather.titan.feature.portal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minestom.server.coordinate.Point;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.Portal;

/**
 * Decides per move whether a player enters a portal that should now send them away.
 *
 * <p>A portal fires on the transition from "not touched" to "touched" only, so standing in it
 * never repeats the delivery; after a delivery the player has a fixed cooldown against every
 * portal, and an entry swallowed by the cooldown is not made up for later. A rejected player
 * (missing permission) gets neither a delivery nor a cooldown. State is per player and owned by
 * this instance.
 */
final class PortalTrigger {

    /** Fixed on purpose: a knob nobody needs would only be one more thing to mis-set. */
    static final Duration COOLDOWN = Duration.ofSeconds(3);

    /** Per player: which portals the last move touched, and until when deliveries are blocked. */
    private static final class PlayerState {
        Set<String> touched = Set.of();
        Instant cooldownUntil = Instant.MIN;
    }

    private final PortalIndex index;
    private final PermissionService permissions;
    private final Clock clock;
    private final Consumer<Portal> onDenied;
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    /**
     * @param onDenied told each portal a player entered without the permission; a refusal during
     *                 the cooldown is not reported
     */
    PortalTrigger(PortalIndex index, PermissionService permissions, Clock clock, Consumer<Portal> onDenied) {
        this.index = Objects.requireNonNull(index, "index");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.onDenied = Objects.requireNonNull(onDenied, "onDenied");
    }

    /**
     * Handles one move from {@code from} (the player's current feet) to {@code to}.
     *
     * @return the portal to deliver the player to; the cooldown starts with it, so the caller has
     *         to deliver
     */
    Optional<Portal> onMove(UUID playerId, Point from, Point to) {
        PlayerState state = this.states.computeIfAbsent(playerId, id -> new PlayerState());
        Instant now = this.clock.instant();
        boolean coolingDown = now.isBefore(state.cooldownUntil);
        Set<String> touchedNow = new HashSet<>();
        Portal entered = null;
        for (Portal portal : this.index.candidates(from, to)) {
            if (!portal.shape().crossedBy(from, to)) {
                continue;
            }
            touchedNow.add(portal.id());
            if (state.touched.contains(portal.id())) {
                continue;
            }
            if (!mayUse(playerId, portal)) {
                if (!coolingDown) {
                    this.onDenied.accept(portal);
                }
            } else if (entered == null) {
                entered = portal;
            }
        }
        state.touched = touchedNow;
        if (entered == null || coolingDown) {
            return Optional.empty();
        }
        state.cooldownUntil = now.plus(COOLDOWN);
        return Optional.of(entered);
    }

    /** Drops everything remembered about {@code player}; called when they disconnect. */
    void forget(UUID playerId) {
        this.states.remove(playerId);
    }

    private boolean mayUse(UUID playerId, Portal portal) {
        return portal.permission() == null || this.permissions.check(playerId, portal.permission()) == PermissionResult.ALLOWED;
    }
}
