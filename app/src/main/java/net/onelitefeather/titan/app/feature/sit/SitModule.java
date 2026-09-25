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

import io.avaje.inject.Priority;
import jakarta.inject.Singleton;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.common.event.EntityDismountEvent;

/**
 * Lets a player sit down on an allowed block and stand back up again, either by sneaking, by
 * dismounting some other way, or by disconnecting.
 *
 * <p>See {@code design.md}, decision 11, and the {@code lobby-modules} spec, scenario "Sitzen und
 * Aufstehen". The actual seating logic lives in the package-private {@link Seats}.
 *
 * <p>Behaviour, in terms of the four listeners this module registers through
 * {@link ModuleContext#listen}:
 * <ol>
 * <li>{@link PlayerBlockInteractEvent}: clicking a block whose key is in
 * {@link SitConfig#allowedBlocks()} sits the player down there.</li>
 * <li>{@link PlayerPacketEvent}: a sneak ({@link ClientInputPacket#shift()}) input packet sent
 * while riding something fires the shared {@link EntityDismountEvent} - the same event type
 * {@code common.event} already defines, kept as an explicitly allowed cross-feature
 * dependency.</li>
 * <li>{@link EntityDismountEvent}: if the dismounting rider is a sitting player, stands them back
 * up.</li>
 * <li>{@link PlayerDisconnectEvent}: stands a disconnecting player back up, so no seat entity is
 * left behind.</li>
 * </ol>
 */
@Singleton
@Priority(500)
public final class SitModule implements LobbyModule {

    @Override
    public String id() {
        return "sit";
    }

    @Override
    public void enable(ModuleContext context) {
        SitConfig config = context.config(SitConfig.class, SitConfig.DEFAULTS);
        List<Key> allowedBlocks = config.allowedBlocks();
        Seats seats = new Seats(config.offset());

        context.listen(PlayerBlockInteractEvent.class, event -> {
            if (isAllowedBlock(allowedBlocks, event.getBlock().key())) {
                seats.sit(event.getPlayer(), event.getBlockPosition());
            }
        });

        // Stand up (dismount) when the sitting player presses sneak. Use the shift() accessor
        // rather than testing the raw flags: the sneak bit is 0x20, not 0x02 (that is backward),
        // and shift() also matches when other movement keys are held at the same time.
        context.listen(PlayerPacketEvent.class, event -> {
            if (event.getPacket() instanceof ClientInputPacket input && input.shift()) {
                Entity vehicle = event.getPlayer().getVehicle();
                if (vehicle != null) {
                    EventDispatcher.call(new EntityDismountEvent(event.getPlayer(), vehicle));
                }
            }
        });

        context.listen(EntityDismountEvent.class, event -> {
            if (event.rider() instanceof Player player && seats.isSitting(player)) {
                seats.standUp(player);
            }
        });

        context.listen(PlayerDisconnectEvent.class, event -> seats.standUp(event.getPlayer()));
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
