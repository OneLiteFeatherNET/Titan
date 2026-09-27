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
package net.onelitefeather.titan.app.feature.protection;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.common.utils.Cancelable;

/**
 * Protects the lobby from being modified by a player: nobody may pick up, drop, or swap items,
 * click inside an inventory, or break or place a block.
 *
 * <p>This module has no state and no configuration - it simply cancels every
 * {@link PickupItemEvent}, {@link InventoryPreClickEvent}, {@link PlayerBlockBreakEvent},
 * {@link PlayerBlockPlaceEvent}, {@link PlayerSwapItemEvent} and {@link ItemDropEvent}
 * unconditionally, mirroring what {@code Titan#initListeners()} wired directly onto the shared
 * event node before this module existed.
 *
 * <p>Cancelling an event does not stop it from reaching listeners registered elsewhere: Minestom
 * keeps walking the rest of the listener chain regardless of {@link
 * net.minestom.server.event.trait.CancellableEvent#isCancelled()}. What a cancellation does affect
 * is any single {@code Consumer}-based listener registered through {@link FeatureNode#on} for a
 * cancellable event type - such a listener checks {@code isCancelled()} right before running and
 * skips its own body if the event is already cancelled by the time it is invoked. A feature that
 * must react to a cancellable event regardless of this module's cancellation uses
 * {@link FeatureNode#onIncludingCancelled} instead of {@link FeatureNode#on} for that listener,
 * which keeps the two features independent of each other's start order (see {@code lobby-modules}
 * spec, "Module sind voneinander unabhängig") - no feature needs that today. The navigator's own
 * menu, for one, never competes with this module's cancellation of {@link InventoryPreClickEvent}
 * in the first place: its click handling runs through Aves, mapped directly onto the inventory it
 * opens rather than through a listener on this module's or its own event node, and Minestom
 * dispatches that mapped handler before any regular event node - including this module's - ever
 * sees the click (see {@code feature.navigator.NavigatorModule}'s Javadoc).
 */
@Singleton
public final class ProtectionModule {

    /**
     * This feature's position among its sibling {@link FeatureNode}s; unchanged from the old
     * {@code @Priority(100)}.
     */
    static final int EVENT_PRIORITY = 100;

    private static final String ID = "protection";

    private final EventNode<Event> titan;
    private FeatureNode node;

    /**
     * @param titan the shared event node this feature's own node attaches under
     */
    public ProtectionModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
    }

    /** Attaches this feature's own event node and registers every cancelling listener on it. */
    @PostConstruct
    void start() {
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PickupItemEvent.class, Cancelable::cancel).on(InventoryPreClickEvent.class, Cancelable::cancel).on(PlayerBlockBreakEvent.class, Cancelable::cancel).on(PlayerBlockPlaceEvent.class, Cancelable::cancel).on(PlayerSwapItemEvent.class, Cancelable::cancel).on(ItemDropEvent.class, Cancelable::cancel);
    }

    /** Detaches this feature's own event node, so none of the listeners above run again. */
    @PreDestroy
    void stop() {
        this.node.close();
    }
}
