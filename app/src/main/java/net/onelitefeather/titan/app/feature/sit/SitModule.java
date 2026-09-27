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
package net.onelitefeather.titan.app.feature.sit;

import io.avaje.config.Config;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.common.event.EntityDismountEvent;

/**
 * Lets a player sit down on an allowed block and stand back up again, either by sneaking, by
 * dismounting some other way, or by disconnecting.
 *
 * <p>See {@code design.md}, decision 11, and the {@code lobby-modules} spec, scenario "Sitzen und
 * Aufstehen". The actual seating logic lives in the package-private {@link Seats}.
 *
 * <p>Behaviour, in terms of the four listeners this module registers through its own
 * {@link FeatureNode}:
 * <ol>
 * <li>{@link PlayerBlockInteractEvent}: clicking a block whose key is in the configured
 * {@code sit.allowedBlocks} sits the player down there.</li>
 * <li>{@link PlayerPacketEvent}: a sneak ({@link ClientInputPacket#shift()}) input packet sent
 * while riding something fires the shared {@link EntityDismountEvent} - the same event type
 * {@code common.event} already defines, kept as an explicitly allowed cross-feature
 * dependency.</li>
 * <li>{@link EntityDismountEvent}: if the dismounting rider is a sitting player, stands them back
 * up.</li>
 * <li>{@link PlayerDisconnectEvent}: stands a disconnecting player back up, so no seat entity is
 * left behind.</li>
 * </ol>
 *
 * <p>An {@code @Singleton} bean (see
 * {@code openspec/changes/dissolve-module-platform/design.md}, decision 1): {@link #start()}
 * attaches this feature's own {@link FeatureNode} once the container builds this bean, and
 * {@link #stop()} detaches it again when the container is closed.
 */
@Singleton
public final class SitModule {

    static final int EVENT_PRIORITY = 500;

    private static final String ID = "sit";

    private final EventNode<Event> titan;
    private FeatureNode node;

    /**
     * @param titan the shared event node this feature's own node attaches under
     */
    public SitModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        this.titan = Objects.requireNonNull(titan, "titan");
    }

    @PostConstruct
    void start() {
        // Abort startup on an invalid value (unchanged behaviour); neither result is kept - the
        // PlayerBlockInteractEvent listener below reads the live values again on every
        // interaction (see design.md, decision 1).
        Config.getAs(SitSettings.OFFSET_X_KEY, Double::parseDouble);
        Config.getAs(SitSettings.OFFSET_Y_KEY, Double::parseDouble);
        Config.getAs(SitSettings.OFFSET_Z_KEY, Double::parseDouble);
        SitSettings.allowedBlocks(Config.list().of(SitSettings.ALLOWED_BLOCKS_KEY).stream().map(SitSettings::parseBlock).toList());
        Seats seats = new Seats();

        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY);

        this.node.on(PlayerBlockInteractEvent.class, event -> {
            // Live, unvalidated read on every interaction (see design.md, decision 1): the
            // strict check above only ever runs once, at startup (refactor/drop-runtime-fallback).
            List<Key> allowedBlocks = Config.list().of(SitSettings.ALLOWED_BLOCKS_KEY).stream().map(SitSettings::parseBlock).toList();
            if (isAllowedBlock(allowedBlocks, event.getBlock().key())) {
                double x = Config.getAs(SitSettings.OFFSET_X_KEY, Double::parseDouble);
                double y = Config.getAs(SitSettings.OFFSET_Y_KEY, Double::parseDouble);
                double z = Config.getAs(SitSettings.OFFSET_Z_KEY, Double::parseDouble);
                seats.sit(event.getPlayer(), event.getBlockPosition(), new Vec(x, y, z));
            }
        });

        // Stand up (dismount) when the sitting player presses sneak. Use the shift() accessor
        // rather than testing the raw flags: the sneak bit is 0x20, not 0x02 (that is backward),
        // and shift() also matches when other movement keys are held at the same time.
        this.node.on(PlayerPacketEvent.class, event -> {
            if (event.getPacket() instanceof ClientInputPacket input && input.shift()) {
                Entity vehicle = event.getPlayer().getVehicle();
                if (vehicle != null) {
                    EventDispatcher.call(new EntityDismountEvent(event.getPlayer(), vehicle));
                }
            }
        });

        this.node.on(EntityDismountEvent.class, event -> {
            if (event.rider() instanceof Player player && seats.isSitting(player)) {
                seats.standUp(player);
            }
        });

        this.node.on(PlayerDisconnectEvent.class, event -> seats.standUp(event.getPlayer()));
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }

    /**
     * Whether {@code block} is one of {@code allowedBlocks} - the pure rule behind the
     * {@link PlayerBlockInteractEvent} listener, extracted so it can be unit-tested without an
     * {@code Env}.
     *
     * @param allowedBlocks the configured allowed block keys
     * @param block         the key of the block a player interacted with
     * @return {@code true} if a player may sit down on {@code block}
     */
    static boolean isAllowedBlock(List<Key> allowedBlocks, Key block) {
        return allowedBlocks.contains(block);
    }
}
