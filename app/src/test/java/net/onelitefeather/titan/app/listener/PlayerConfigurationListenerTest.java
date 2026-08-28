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
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class PlayerConfigurationListenerTest {

    @DisplayName("Test set spawning instance")
    @Test
    void testSetSpawningInstance(Env env) {
        MapProvider mapProvider = mock(MapProvider.class);

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, new PlayerConfigurationListener(mapProvider));
        InstanceContainer instance = (InstanceContainer) env.createFlatInstance();

        when(mapProvider.getInstance()).thenReturn(instance);
        Player player = env.createPlayer(instance);
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);

        // This solution is not the best but standard Minestom moment
        Thread.startVirtualThread(() -> {
            MinecraftServer.getGlobalEventHandler().call(event);
            Mockito.verify(mapProvider, atLeastOnce()).getInstance();
            assertEquals(instance, event.getSpawningInstance());
        });
    }

}
