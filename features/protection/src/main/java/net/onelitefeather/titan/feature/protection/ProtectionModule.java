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
package net.onelitefeather.titan.feature.protection;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Objects;
import java.util.function.Consumer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.utils.Cancelable;

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
    private static final String DENIED_METRIC = "protection.denied";
    private static final AttributeKey<String> EVENT = AttributeKey.stringKey("event");

    private final EventNode<Event> titan;
    private final Telemetry telemetry;
    private final LongCounter denied;
    private FeatureNode node;

    public ProtectionModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Telemetry telemetry) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.denied = telemetry.meter().counterBuilder(DENIED_METRIC).setUnit("{event}").setDescription("Events cancelled by the lobby protection").build();
    }

    @PostConstruct
    void start() {
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PickupItemEvent.class, deny("pickup")).on(InventoryPreClickEvent.class, deny("inventory_click")).on(PlayerBlockBreakEvent.class, deny("block_break")).on(PlayerBlockPlaceEvent.class, deny("block_place")).on(PlayerSwapItemEvent.class, deny("item_swap")).on(ItemDropEvent.class, deny("item_drop"));
    }

    /**
     * Cancels the event and counts it under {@code event}. No span: these fire far too often, and
     * the counter's only attribute is the fixed event name.
     */
    private <E extends CancellableEvent> Consumer<E> deny(String event) {
        Attributes attributes = Attributes.of(EVENT, event);
        return cancellable -> {
            this.denied.add(1, attributes);
            Cancelable.cancel(cancellable);
        };
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }
}
