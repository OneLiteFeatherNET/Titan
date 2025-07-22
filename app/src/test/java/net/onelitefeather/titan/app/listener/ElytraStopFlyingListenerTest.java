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
package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class ElytraStopFlyingListenerTest {

    @DisplayName("Test if the is air item is set in the offhand when the player stops flying with the elytra")
    @Disabled
    @Test
    void testElytraStopFlyingListener(Env env) {
        Instance flatInstance = env.createFlatInstance();
        var realPlayer = env.createPlayer(flatInstance);
        var realInventory = realPlayer.getInventory();
        Player player = spy(realPlayer);
        doReturn(spy(realInventory)).when(player).getInventory();
        env.tick();
        MinecraftServer.getGlobalEventHandler().addListener(PlayerStopFlyingWithElytraEvent.class, new ElytraStopFlyingListener());
        MinecraftServer.getGlobalEventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

        Assertions.assertEquals(ItemStack.AIR, player.getItemInOffHand());
        verify(player.getInventory(), atLeastOnce()).update();
    }

}
