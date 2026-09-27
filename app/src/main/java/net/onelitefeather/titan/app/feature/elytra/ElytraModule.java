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
 * Moves today's elytra flight and firework boost - {@code ElytraStartFlyingListener}, {@code
 * ElytraStopFlyingListener} and {@code ElytraBoostListener} on {@code main} - into a bean feature
 * (see {@code openspec/changes/lobby-feature-modules}, task 6.7 and 13.2, and {@code
 * openspec/changes/dissolve-module-platform/design.md}, decisions 1-3).
 *
 * <p>The two {@link net.onelitefeather.titan.app.module.item.LobbyItem}s this feature owns -
 * {@code titan:elytra} (fixed to {@link net.minestom.server.entity.EquipmentSlot#CHESTPLATE}) and
 * {@code titan:firework} (no fixed place, since it only ever lives in the offhand while a player is
 * gliding) - are contributed as {@code @Bean}s by {@link ElytraLobbyItems} instead of being
 * registered here directly. This module hands the {@link LobbyItems}-stamped firework stack into
 * the offhand itself on {@link PlayerStartFlyingWithElytraEvent} and takes it back on
 * {@link PlayerStopFlyingWithElytraEvent}, exactly as the {@code lobby-hotbar} spec requires for
 * "Items mit wechselndem Platz" - {@link LobbyItems#stack(net.kyori.adventure.key.Key)} is the only
 * way to get a stack that still carries the identity tag {@link ElytraLobbyItems}'s own use handler
 * needs to be dispatched to at all.
 *
 * <p>Per-player boost state lives in the shared {@link FireworkBoostTracker} bean - shared with
 * {@link ElytraLobbyItems#firework(FireworkBoostTracker)}'s use handler, which is the only thing
 * that ever calls {@link FireworkBoostTracker#requestBoost} - cleared on stop-flying and on
 * {@link PlayerDisconnectEvent} so it never leaks a player who can no longer be boosted, and
 * advanced once per tick through the injected {@link Scheduler}, scheduled in {@link #start()} and
 * cancelled in {@link #stop()}, after this feature's own {@link FeatureNode} has already been
 * detached.
 */
@Singleton
public final class ElytraModule {

    /**
     * This feature's position among its sibling {@link FeatureNode}s; unchanged from the old
     * {@code @Priority(700)}.
     */
    static final int EVENT_PRIORITY = 700;

    private static final String ID = "elytra";

    private final EventNode<Event> titan;
    private final LobbyItems lobbyItems;
    private final FireworkBoostTracker boosts;
    private final Scheduler scheduler;
    private FeatureNode node;
    private Task task;

    /**
     * @param titan      the shared event node this feature's own node attaches under
     * @param lobbyItems the platform-wide item registry the stamped {@code titan:firework} stack
     *                   is read from
     * @param boosts     the shared boost tracker also used by {@link ElytraLobbyItems}'s firework
     *                   use handler
     * @param scheduler  the scheduler {@link FireworkBoostTracker#advance()} is scheduled on
     */
    public ElytraModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbyItems lobbyItems, FireworkBoostTracker boosts, Scheduler scheduler) {
        this.titan = titan;
        this.lobbyItems = lobbyItems;
        this.boosts = boosts;
        this.scheduler = scheduler;
    }

    /**
     * Validates the startup configuration (unchanged behaviour; neither result is kept - the
     * firework's use handler reads the live values again on every boost, see {@link
     * ElytraLobbyItems}), attaches this feature's own event node and schedules
     * {@link FireworkBoostTracker#advance()} once per tick.
     */
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

    /**
     * Detaches this feature's own event node first, so no more flight event can schedule or read
     * boost state, then cancels the per-tick task.
     */
    @PreDestroy
    void stop() {
        this.node.close();
        this.task.cancel();
    }
}
