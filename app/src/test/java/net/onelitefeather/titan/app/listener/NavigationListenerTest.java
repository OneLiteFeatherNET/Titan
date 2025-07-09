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
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import net.onelitefeather.titan.app.testutils.DummyDeliver;
import net.onelitefeather.titan.common.utils.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MicrotusExtension.class)
class NavigationListenerTest {

    @Test
    @DisplayName("Test has the navigator is opened when the player uses the player teleporter")
    void testNavigationListenerForClicked(Env env) {
        NavigationHelper navigationHelper = spy(NavigationHelper.instance(DummyDeliver.instance()));
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, new NavigationListener(navigationHelper));
        PlayerUseItemEvent playerUseItemEvent = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_TELEPORTER, 1);
        MinecraftServer.getGlobalEventHandler().call(playerUseItemEvent);

        verify(navigationHelper).openNavigator(player);
    }

}
