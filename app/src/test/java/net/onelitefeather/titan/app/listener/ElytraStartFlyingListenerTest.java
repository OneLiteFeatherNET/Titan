/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.utils.Items;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class ElytraStartFlyingListenerTest {

    @DisplayName("Test if the is firerwork item is set in the offhand")
    @Disabled
    @Test
    void testElytraStartFlyingListener(Env env) {
        Instance flatInstance = env.createFlatInstance();
        var realPlayer = env.createPlayer(flatInstance);
        var realInventory = realPlayer.getInventory();
        Player player = spy(realPlayer);
        doReturn(spy(realInventory)).when(player).getInventory();
        env.tickWhile(() -> true, Duration.of(10, ChronoUnit.SECONDS));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerStartFlyingWithElytraEvent.class, new ElytraStartFlyingListener());
        MinecraftServer.getGlobalEventHandler().call(new PlayerStartFlyingWithElytraEvent(player));

        Assertions.assertEquals(Items.PLAYER_FIREWORK, player.getItemInOffHand());
        verify(player.getInventory(), atLeastOnce()).update();
    }

}
