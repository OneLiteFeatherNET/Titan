/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
import net.onelitefeather.titan.app.testutils.TestFeatureGate;
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
        NavigationHelper navigationHelper = spy(NavigationHelper.instance(DummyDeliver.instance(), TestFeatureGate.create().gate()));
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, new NavigationListener(navigationHelper));
        PlayerUseItemEvent playerUseItemEvent = new PlayerUseItemEvent(player, PlayerHand.MAIN, Items.PLAYER_TELEPORTER, 1);
        MinecraftServer.getGlobalEventHandler().call(playerUseItemEvent);

        verify(navigationHelper).openNavigator(player);
    }

}
