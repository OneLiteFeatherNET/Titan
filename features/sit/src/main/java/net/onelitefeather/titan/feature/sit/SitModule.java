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
package net.onelitefeather.titan.feature.sit;

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
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.event.EntityDismountEvent;

/**
 * Lets a player sit down on an allowed block and stand back up again, by sneaking, by dismounting
 * some other way, or by disconnecting; the seating logic itself lives in {@link Seats}.
 */
@Singleton
public final class SitModule {

    static final int EVENT_PRIORITY = 500;

    private static final String ID = "sit";

    private final EventNode<Event> titan;
    private FeatureNode node;

    public SitModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        this.titan = Objects.requireNonNull(titan, "titan");
    }

    @PostConstruct
    void start() {
        // Validates the offset config at startup so a bad value aborts startup immediately; the
        // PlayerBlockInteractEvent listener below re-reads the live values on every interaction.
        Config.getAs(SitSettings.OFFSET_X_KEY, Double::parseDouble);
        Config.getAs(SitSettings.OFFSET_Y_KEY, Double::parseDouble);
        Config.getAs(SitSettings.OFFSET_Z_KEY, Double::parseDouble);
        SitSettings.allowedBlocks(Config.list().of(SitSettings.ALLOWED_BLOCKS_KEY).stream().map(SitSettings::parseBlock).toList());
        Seats seats = new Seats();

        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY);

        this.node.on(PlayerBlockInteractEvent.class, event -> {
            // Live, unvalidated read on every interaction - the strict check above only runs
            // once, at startup.
            List<Key> allowedBlocks = Config.list().of(SitSettings.ALLOWED_BLOCKS_KEY).stream().map(SitSettings::parseBlock).toList();
            if (isAllowedBlock(allowedBlocks, event.getBlock().key())) {
                double x = Config.getAs(SitSettings.OFFSET_X_KEY, Double::parseDouble);
                double y = Config.getAs(SitSettings.OFFSET_Y_KEY, Double::parseDouble);
                double z = Config.getAs(SitSettings.OFFSET_Z_KEY, Double::parseDouble);
                seats.sit(event.getPlayer(), event.getBlockPosition(), new Vec(x, y, z));
            }
        });

        // Uses shift() rather than the raw flags: the sneak bit is 0x20, not 0x02, and shift()
        // also matches when other movement keys are held at once.
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

    // Extracted as a pure function so the block-allow rule can be unit-tested without an Env.
    static boolean isAllowedBlock(List<Key> allowedBlocks, Key block) {
        return allowedBlocks.contains(block);
    }
}
