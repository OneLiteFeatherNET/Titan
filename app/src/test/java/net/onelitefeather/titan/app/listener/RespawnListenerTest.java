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
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.helper.NavigationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MicrotusExtension.class)
class RespawnListenerTest {

    @DisplayName("test if the player gets the items after respawn")
    @Test
    void testRespawnListener(Env env) {
        Instance flatInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(flatInstance);

        NavigationHelper navigationHelper = mock(NavigationHelper.class);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerRespawnEvent.class, new RespawnListener(navigationHelper));
        MinecraftServer.getGlobalEventHandler().call(new PlayerRespawnEvent(player));

        verify(navigationHelper).setItems(eq(player));
    }

}
