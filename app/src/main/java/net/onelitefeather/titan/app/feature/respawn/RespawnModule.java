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
package net.onelitefeather.titan.app.feature.respawn;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.item.LobbyItems;

/**
 * Handles a lobby player's death and respawn: blanks the death text and respawns immediately on
 * {@link PlayerDeathEvent}, then hands back the standard loadout on {@link PlayerRespawnEvent}.
 *
 * <p>{@code Player#respawn()} is a no-op until {@code kill()} finishes marking the player dead, so
 * the respawn is deferred one tick via the player's own scheduler instead of called directly.
 */
@Singleton
public final class RespawnModule {

    static final int EVENT_PRIORITY = 300;

    private static final String ID = "respawn";

    private final EventNode<Event> titan;
    private final LobbyItems lobbyItems;
    private FeatureNode node;

    public RespawnModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbyItems lobbyItems) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.lobbyItems = Objects.requireNonNull(lobbyItems, "lobbyItems");
    }

    @PostConstruct
    void start() {
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PlayerDeathEvent.class, RespawnModule::onDeath).on(PlayerRespawnEvent.class, event -> this.lobbyItems.equip(event.getPlayer()));
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }

    private static void onDeath(PlayerDeathEvent event) {
        event.setDeathText(Component.empty());
        Player player = event.getPlayer();
        // The player's own scheduler drops the task automatically on disconnect, and respawn()
        // already checks isDead(), so no extra double-respawn guard is needed here.
        player.scheduler().scheduleNextTick(player::respawn);
    }
}
