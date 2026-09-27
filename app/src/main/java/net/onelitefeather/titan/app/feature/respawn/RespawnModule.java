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
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.app.module.item.ItemRegistry;

/**
 * Handles a lobby player's death and respawn.
 *
 * <p>On {@link PlayerDeathEvent}, this module blanks the death text and respawns the player right
 * away - there is no death screen in the lobby. On {@link PlayerRespawnEvent}, it hands the player
 * back the platform's standard loadout through {@link ItemRegistry#equip(Player)}, the same call
 * the spawn module makes on join.
 *
 * <p>Minestom's {@code Player#kill()} dispatches {@link PlayerDeathEvent} <em>before</em> it marks
 * the player dead ({@code Player#isDead()} only flips to {@code true} once the event has been
 * handled), and {@code Player#respawn()} is a no-op while {@code isDead()} is still {@code false}.
 * Calling {@code respawn()} straight from the {@link PlayerDeathEvent} listener would therefore
 * silently do nothing. This module instead defers the respawn to the next tick, via the player's
 * own {@link net.minestom.server.timer.Scheduler} - by then {@code kill()} has finished and
 * {@code isDead()} is {@code true}, so {@code respawn()} actually runs. Scheduling on the player's
 * own scheduler (instead of a task this feature would have to cancel itself) also means the task is
 * dropped for free if the player disconnects before the next tick, without this module having to
 * track it. No extra double-respawn guard is needed: {@code respawn()} already checks
 * {@code isDead()} itself, so a player who is revived by some other means before the scheduled
 * respawn runs is simply left alone.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/specs/lobby-modules/spec.md}, scenario
 * "Tod ohne Nachricht", and {@code specs/lobby-hotbar/spec.md}, scenario "Ausstattung nach
 * Respawn". This module has no configuration of its own.
 *
 * <p>An {@code @Singleton} bean (see
 * {@code openspec/changes/dissolve-module-platform/design.md}, decision 1): {@link #start()}
 * attaches this feature's own {@link FeatureNode} once the container builds this bean, and
 * {@link #stop()} detaches it again when the container is closed.
 */
@Singleton
public final class RespawnModule {

    static final int EVENT_PRIORITY = 300;

    private final EventNode<Event> titan;
    private final ItemRegistry itemRegistry;
    private FeatureNode node;

    /**
     * @param titan        the shared event node this feature's own node attaches under
     * @param itemRegistry equips the respawning player with the platform's standard loadout.
     *                     TODO(dissolve-module-platform, task 3.1): read from the
     *                     {@code LobbyItems} bean directly once every feature's items are beans -
     *                     until then this bridges to items that not-yet-migrated features (e.g.
     *                     navigator, elytra) still register with the old platform, see
     *                     {@code openspec/changes/dissolve-module-platform/tasks.md} execution
     *                     plan
     */
    public RespawnModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, ItemRegistry itemRegistry) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry");
    }

    @PostConstruct
    void start() {
        this.node = FeatureNode.attach(this.titan, "respawn", EVENT_PRIORITY).on(PlayerDeathEvent.class, RespawnModule::onDeath).on(PlayerRespawnEvent.class, event -> this.itemRegistry.equip(event.getPlayer()));
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }

    private static void onDeath(PlayerDeathEvent event) {
        event.setDeathText(Component.empty());
        Player player = event.getPlayer();
        player.scheduler().scheduleNextTick(player::respawn);
    }
}
