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
package net.onelitefeather.titan.app.feature.elytra;

import io.avaje.config.Config;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.app.module.item.LobbyItems;

/**
 * The {@code elytra} feature: flight and firework boost. {@link ElytraLobbyItems} contributes its
 * two {@link net.onelitefeather.titan.app.module.item.LobbyItem}s; this module hands the stamped
 * firework stack into a player's offhand on {@link PlayerStartFlyingWithElytraEvent} and takes it
 * back on {@link PlayerStopFlyingWithElytraEvent}. Per-player boost state lives in the shared
 * {@link FireworkBoostTracker}, advanced once per tick and cleared on stop-flying or disconnect.
 */
@Singleton
public final class ElytraModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 700;

    private static final String ID = "elytra";

    private final EventNode<Event> titan;
    private final LobbyItems lobbyItems;
    private final FireworkBoostTracker boosts;
    private final Scheduler scheduler;
    private FeatureNode node;
    private Task task;

    public ElytraModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbyItems lobbyItems, FireworkBoostTracker boosts, Scheduler scheduler) {
        this.titan = titan;
        this.lobbyItems = lobbyItems;
        this.boosts = boosts;
        this.scheduler = scheduler;
    }

    @PostConstruct
    void start() {
        int burnDurationTicksAtStartup = Config.getAs(ElytraSettings.BURN_DURATION_TICKS_KEY, ElytraSettings::burnDurationTicks);
        ElytraSettings.cooldownTicks(Config.getAs(ElytraSettings.COOLDOWN_TICKS_KEY, Integer::parseInt), burnDurationTicksAtStartup);

        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PlayerStartFlyingWithElytraEvent.class, event -> event.getPlayer().setItemInOffHand(this.lobbyItems.stack(ElytraLobbyItems.FIREWORK_KEY))).on(PlayerStopFlyingWithElytraEvent.class, event -> {
            event.getPlayer().setItemInOffHand(ItemStack.AIR);
            this.boosts.forget(event.getPlayer().getUuid());
        }).on(PlayerDisconnectEvent.class, event -> this.boosts.forget(event.getPlayer().getUuid()));

        this.task = this.scheduler.scheduleTask(this.boosts::advance, TaskSchedule.tick(1), TaskSchedule.tick(1));
    }

    @PreDestroy
    void stop() {
        // Detach the node before cancelling the task, so no flight event can touch tracker state
        // after the task is gone.
        this.node.close();
        this.task.cancel();
    }
}
