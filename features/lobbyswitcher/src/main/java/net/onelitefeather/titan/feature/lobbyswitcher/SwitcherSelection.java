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

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What happens when a player clicks a lobby: the target is checked against a fresh reading off the
 * tick, and the verdict is applied on the next tick. Only {@link SwitcherClickDecision#SEND} moves
 * the player; every other verdict tells them why and has the list read again.
 */
final class SwitcherSelection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitcherSelection.class);

    private final PlayerCounts counts;
    private final Scheduler scheduler;
    private final Executor executor;
    private final Deliver deliver;
    private final LobbySwitcherMessages messages;
    private final LobbySwitcherTelemetry telemetry;
    private final Runnable refresh;
    // A click is checked and applied over two steps; a second click in between must not send twice.
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    SwitcherSelection(PlayerCounts counts, Scheduler scheduler, Executor executor, Deliver deliver, LobbySwitcherMessages messages, LobbySwitcherTelemetry telemetry, Runnable refresh) {
        this.counts = Objects.requireNonNull(counts, "counts must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.deliver = Objects.requireNonNull(deliver, "deliver must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.refresh = Objects.requireNonNull(refresh, "refresh must not be null");
    }

    /**
     * The player clicked the lobby {@code target} in a list shown from {@code own}'s point of view.
     */
    void select(Player player, LobbyIdentity own, String target) {
        if (!this.pending.add(player.getUuid())) {
            return;
        }
        try {
            this.executor.execute(() -> check(player, own, target));
        } catch (RuntimeException e) {
            this.pending.remove(player.getUuid());
            LOGGER.warn("Starting the check of lobby '{}' failed: {}", target, e.toString());
        }
    }

    private void check(Player player, LobbyIdentity own, String target) {
        SwitcherClickDecision decision = SwitcherClickDecision.decide(target, own, () -> this.counts.running(SourceType.TASK, own.task()));
        this.scheduler.scheduleNextTick(() -> conclude(player, target, decision));
    }

    private void conclude(Player player, String target, SwitcherClickDecision decision) {
        this.pending.remove(player.getUuid());
        if (!player.isOnline()) {
            return;
        }
        this.telemetry.select(player.getUuid(), target, decision, () -> {
            if (decision == SwitcherClickDecision.SEND) {
                this.deliver.sendPlayer(player, DeliverComponent.serverBuilder().serverName(target).player(player).build());
                player.closeInventory();
            } else {
                this.refresh.run();
            }
            player.sendMessage(this.messages.click(Objects.requireNonNullElse(player.getLocale(), Locale.ENGLISH), decision, target));
        });
    }
}
