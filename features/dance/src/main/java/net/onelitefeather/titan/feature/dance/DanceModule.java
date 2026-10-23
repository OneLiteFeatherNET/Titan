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
package net.onelitefeather.titan.feature.dance;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.feature.dance.DanceService.StartResult;
import net.onelitefeather.titan.feature.dance.DanceService.StopReason;
import net.onelitefeather.pica.dialog.tag.DialogTagRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets a player dance from the quick action (G) or {@code /tanz}: the quick action opens a
 * per-player choice, a click on a dance starts it, and any movement, input, attack, interaction,
 * leaving the instance or disconnect ends it. The dancing itself lives in {@link DanceService}.
 */
@Singleton
public final class DanceModule {

    static final int EVENT_PRIORITY = 650;

    static final String ID = "dance";

    private static final Logger LOGGER = LoggerFactory.getLogger(DanceModule.class);

    private final EventNode<Event> titan;
    private final Telemetry telemetry;
    private final DanceMessages messages;
    private final DanceMenu menu;
    private final DanceService service;
    private DialogTagRegistry dialogTags;
    private FeatureNode node;

    @Inject
    public DanceModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, PermissionService permissions, Scheduler scheduler, Clock clock, Telemetry telemetry) {
        this(titan, permissions, scheduler, clock, telemetry, MinestomDanceEffects::new);
    }

    DanceModule(EventNode<Event> titan, PermissionService permissions, Scheduler scheduler, Clock clock, Telemetry telemetry, BiFunction<Player, LivingEntity, DanceEffects> effectsFor) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.messages = new DanceMessages();
        DancePermissions dancePermissions = new DancePermissions(Objects.requireNonNull(permissions, "permissions"));
        this.menu = new DanceMenu(this.messages, dancePermissions);
        this.service = new DanceService(Objects.requireNonNull(scheduler, "scheduler"), new DanceCooldown(Objects.requireNonNull(clock, "clock")), dancePermissions, new DanceTelemetry(telemetry), effectsFor);
    }

    @PostConstruct
    void start() {
        // Runs while the BeanScope is built, before the server accepts a connection: registry
        // data is sent to every client during the configuration phase.
        this.dialogTags = DialogTagRegistry.of(MinecraftServer.getDialogRegistry());
        this.dialogTags.addQuickAction(DanceKeys.DIALOG, QuickActionDialog.quickAction());
        this.messages.register();

        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerCustomClickEvent.class, this::onCustomClick).on(PlayerPacketEvent.class, this::onPacket).on(PlayerSpawnEvent.class, this::onSpawn).on(PlayerMoveEvent.class, event -> this.service.onMove(event.getPlayer().getUuid(), event.getNewPosition())).onIncludingCancelled(EntityAttackEvent.class, event -> stopIfPlayer(event.getEntity(), StopReason.INTERACTED)).onIncludingCancelled(PlayerHandAnimationEvent.class, event -> stopIfPlayer(event.getPlayer(), StopReason.INTERACTED)).onIncludingCancelled(PlayerBlockInteractEvent.class, event -> stopIfPlayer(event.getPlayer(), StopReason.INTERACTED)).onIncludingCancelled(PlayerUseItemEvent.class, event -> stopIfPlayer(event.getPlayer(), StopReason.INTERACTED)).onIncludingCancelled(PlayerEntityInteractEvent.class, event -> stopIfPlayer(event.getPlayer(), StopReason.INTERACTED)).on(RemoveEntityFromInstanceEvent.class, event -> stopIfPlayer(event.getEntity(), StopReason.LEFT_INSTANCE)).on(PlayerDisconnectEvent.class, event -> this.service.disconnected(event.getPlayer().getUuid()));
    }

    @PreDestroy
    void stop() {
        if (this.node != null) {
            this.node.close();
        }
        this.service.stopAll();
        this.messages.close();
    }

    /** Opens the per-player choice for {@code player}; the {@code /tanz} command uses it too. */
    void openMenu(Player player) {
        this.menu.show(player);
    }

    private void onCustomClick(PlayerCustomClickEvent event) {
        Player player = event.getPlayer();
        Key key = event.getKey();
        if (DanceKeys.OPEN.equals(key)) {
            this.menu.show(player);
        } else if (DanceKeys.STOP.equals(key)) {
            this.service.stop(player.getUuid(), StopReason.STOP_BUTTON);
        } else if (DanceKeys.DANCE.equals(key)) {
            requestDance(player, DancePayload.dance(event.getPayload()));
        }
    }

    private void requestDance(Player player, Optional<Dance> requested) {
        if (requested.isEmpty()) {
            LOGGER.debug("Ignoring a dance request without a known id from {}", player.getUuid());
            return;
        }
        StartResult result = this.service.start(player, requested.get());
        switch (result) {
            case BUSY ->
                player.sendMessage(DanceMessages.text(DanceMessages.BUSY, player.getLocale()));
            case COOLDOWN ->
                player.sendMessage(DanceMessages.text(DanceMessages.COOLDOWN, player.getLocale()));
            case NOT_PERMITTED ->
                LOGGER.debug("Ignoring dance {} from {}: no permission", requested.get().id(), player.getUuid());
            case UNAVAILABLE ->
                LOGGER.debug("Ignoring dance {} from {}: not in an instance", requested.get().id(), player.getUuid());
            case STARTED -> {
                // The dance has started; nothing to tell the player.
            }
        }
    }

    // Pica's eventNode() would do the same, but features may not attach nodes to the event tree.
    private void onSpawn(PlayerSpawnEvent event) {
        if (event.isFirstSpawn()) {
            this.dialogTags.sendTags(event.getPlayer());
        }
    }

    private void onPacket(PlayerPacketEvent event) {
        if (event.getPacket() instanceof ClientInputPacket input && (input.forward() || input.backward() || input.left() || input.right() || input.jump())) {
            this.service.stop(event.getPlayer().getUuid(), StopReason.INPUT);
        }
    }

    private void stopIfPlayer(Entity entity, StopReason reason) {
        if (entity instanceof Player player) {
            this.service.stop(player.getUuid(), reason);
        }
    }
}
