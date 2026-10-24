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
package net.onelitefeather.titan.feature.throwing;

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.listener.PlayerActionListener;
import net.minestom.server.listener.UseItemListener;
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the vanilla signals the carry relies on, through Minestom's own packet listeners rather
 * than hand-built events: the charge item starts a bow draw, and the release reports the ticks it
 * was held. Design task 1.1 and 1.2.
 */
@ExtendWith(MicrotusExtension.class)
class ThrowCarrierPipelineTest {

    @DisplayName("Using the charge item starts a bow draw that lasts the item's consume time")
    @Test
    void chargeItemStartsABowDraw(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        player.getInventory().setItemStack(player.getHeldSlot(), CarrierItem.create());
        List<PlayerBeginItemUseEvent> begins = new ArrayList<>();
        env.process().eventHandler().addListener(PlayerBeginItemUseEvent.class, begins::add);

        UseItemListener.useItemListener(new ClientUseItemPacket(PlayerHand.MAIN, 1, 0, 0), player);

        Assertions.assertEquals(1, begins.size(), "using the charge item must start exactly one use");
        Assertions.assertEquals(ItemAnimation.BOW, begins.getFirst().getAnimation(), "the charge item must draw like a bow");
        Assertions.assertEquals(72_000, begins.getFirst().getItemUseDuration(), "the charge item must be held for the bow's 72000 ticks");
    }

    @DisplayName("Releasing the use reports the number of ticks the item was held")
    @Test
    void releaseReportsTheHeldTicks(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
        player.getInventory().setItemStack(player.getHeldSlot(), CarrierItem.create());
        List<PlayerCancelItemUseEvent> cancels = new ArrayList<>();
        env.process().eventHandler().addListener(PlayerCancelItemUseEvent.class, cancels::add);
        UseItemListener.useItemListener(new ClientUseItemPacket(PlayerHand.MAIN, 1, 0, 0), player);

        for (int tick = 0; tick < 20; tick++) {
            env.tick();
        }
        PlayerActionListener.playerActionListener(new ClientPlayerActionPacket(ClientPlayerActionPacket.Status.UPDATE_ITEM_STATE, Vec.ZERO, BlockFace.TOP, 2), player);

        Assertions.assertEquals(1, cancels.size(), "releasing the use must produce exactly one cancel");
        Assertions.assertEquals(20, cancels.getFirst().getUseDuration(), "the cancel must carry the ticks the item was held");
    }
}
