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
package net.onelitefeather.titan.feature.lobbyswitcher;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.avaje.inject.Profile;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.onelitefeather.titan.core.lobby.LobbyIdentities;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * The lobby switcher: the clock in hotbar slot 8 opens the list of the running lobbies of this
 * task. Only exists as a CloudNet service; elsewhere there is nothing to switch to.
 */
@Singleton
@Profile(LobbySwitcherModule.CLOUDNET)
final class LobbySwitcherModule {

    /** The Avaje profile of a CloudNet service; the runtime's {@code BeanProfiles.CLOUDNET}. */
    static final String CLOUDNET = "cloudnet";

    /** The {@code features.*} flag that decides at startup whether the clock exists. */
    static final String FLAG = "LOBBYSWITCHER";

    /** Free slot between the navigator (400) and the sit module (500). */
    static final int EVENT_PRIORITY = 450;

    private static final String ID = "lobbyswitcher";

    private final EventNode<Event> titan;
    private final SwitcherInventory inventory;
    private final LobbyIdentities identities;
    private final LobbySwitcherMessages messages;
    private final Telemetry telemetry;
    private FeatureNode node;
    private volatile boolean stopped;

    public LobbySwitcherModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, SwitcherInventory inventory, LobbyIdentities identities, LobbySwitcherMessages messages, Telemetry telemetry) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.inventory = Objects.requireNonNull(inventory, "inventory must not be null");
        this.identities = Objects.requireNonNull(identities, "identities must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
    }

    @PostConstruct
    void start() {
        // Aves' click and close listeners do not fire for a per-locale inventory, so the list's
        // clicks and closes are routed from here.
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(InventoryPreClickEvent.class, this.inventory::onClick).on(InventoryCloseEvent.class, this.inventory::onClose);
    }

    /** Idempotent. */
    @PreDestroy
    void stop() {
        if (this.stopped) {
            return;
        }
        this.stopped = true;
        this.node.close();
    }

    void open(Player player) {
        if (this.stopped) {
            return;
        }
        this.identities.self().ifPresent(own -> this.inventory.open(player, own));
    }

    /** Tells the player what their click did, in their own language. */
    void tell(Player player, SwitcherClickDecision decision, String target) {
        player.sendMessage(this.messages.click(player.getLocale(), decision, target));
    }
}
