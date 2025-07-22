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

		MinecraftServer.getGlobalEventHandler().addListener(PlayerRespawnEvent.class,
				new RespawnListener(navigationHelper));
		MinecraftServer.getGlobalEventHandler().call(new PlayerRespawnEvent(player));

		verify(navigationHelper).setItems(eq(player));
	}

}
