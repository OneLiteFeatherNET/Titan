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
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.common.utils.Cancelable;

/**
 * Protects the lobby from being modified: cancels every pickup, drop, swap, inventory-click,
 * block-break, and block-place event unconditionally.
 *
 * <p>Cancelling doesn't stop Minestom from walking the rest of the listener chain, so a feature
 * that must react anyway registers through {@link FeatureNode#onIncludingCancelled} instead of
 * {@link FeatureNode#on}.
 */
@Singleton
public final class ProtectionModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 100;

    private static final String ID = "protection";

    private final EventNode<Event> titan;
    private FeatureNode node;

    public ProtectionModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
    }

    @PostConstruct
    void start() {
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PickupItemEvent.class, Cancelable::cancel).on(InventoryPreClickEvent.class, Cancelable::cancel).on(PlayerBlockBreakEvent.class, Cancelable::cancel).on(PlayerBlockPlaceEvent.class, Cancelable::cancel).on(PlayerSwapItemEvent.class, Cancelable::cancel).on(ItemDropEvent.class, Cancelable::cancel);
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }
}
