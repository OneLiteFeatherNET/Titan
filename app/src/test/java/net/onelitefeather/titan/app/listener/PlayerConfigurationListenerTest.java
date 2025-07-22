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

		MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class,
				new PlayerConfigurationListener(mapProvider));
		InstanceContainer instance = (InstanceContainer) env.createFlatInstance();

		when(mapProvider.getInstance()).thenReturn(instance);
		Player player = env.createPlayer(instance);
		AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
		MinecraftServer.getGlobalEventHandler().call(event);

		Mockito.verify(mapProvider, atLeastOnce()).getInstance();
		assertEquals(instance, event.getSpawningInstance());
	}

}
