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

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.config.InternalAppConfig;
import net.onelitefeather.titan.common.helper.SitHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for SitListener functionality.
 * 
 * Note: Direct testing with PlayerBlockInteractEvent is challenging due to
 * constructor compatibility issues. Instead, we test the core functionality by
 * directly calling SitHelper.sitPlayer().
 */
@ExtendWith(MicrotusExtension.class)
class SitListenerTest {

	@DisplayName("Test if player sits when using SitHelper directly")
	@Test
	void testPlayerSitsWithSitHelper(Env env) {
		// Create a real instance and player
		Instance flatInstance = env.createFlatInstance();
		Player player = env.createPlayer(flatInstance);

		// Use the default AppConfig which has all required values set
		AppConfig appConfig = InternalAppConfig.defaultConfig();

		// Create block position
		Pos blockPos = new Pos(0, 0, 0);

		// Call SitHelper directly
		SitHelper.sitPlayer(player, blockPos, appConfig);

		// Verify that the player is now sitting
		Assertions.assertTrue(SitHelper.isSitting(player),
				"Player should be sitting after calling SitHelper.sitPlayer()");
	}

	@DisplayName("Test if player can be removed from sitting position")
	@Test
	void testPlayerRemovedFromSittingPosition(Env env) {
		// Create a real instance and player
		Instance flatInstance = env.createFlatInstance();
		Player player = env.createPlayer(flatInstance);

		// Use the default AppConfig which has all required values set
		AppConfig appConfig = InternalAppConfig.defaultConfig();

		// Create block position
		Pos blockPos = new Pos(0, 0, 0);

		// Make the player sit
		SitHelper.sitPlayer(player, blockPos, appConfig);

		// Verify that the player is sitting
		Assertions.assertTrue(SitHelper.isSitting(player),
				"Player should be sitting after calling SitHelper.sitPlayer()");

		// Remove the player from sitting position
		SitHelper.removePlayer(player);

		// Verify that the player is no longer sitting
		Assertions.assertFalse(SitHelper.isSitting(player),
				"Player should not be sitting after calling SitHelper.removePlayer()");
	}
}
