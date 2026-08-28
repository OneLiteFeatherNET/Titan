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
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.InternalAppConfig;
import net.onelitefeather.titan.common.event.EntityDismountEvent;
import net.onelitefeather.titan.common.helper.SitHelper;
import net.onelitefeather.titan.common.utils.Tags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

@ExtendWith(MicrotusExtension.class)
class SitDismountListenerTest {

    @DisplayName("Test if player is removed from sitting position on dismount")
    @Test
    void testPlayerRemovedFromSittingPositionOnDismount(Env env) {
        // Create a real instance and player
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        // Create a real AppConfig
        var appConfig = InternalAppConfig.defaultConfig();

        // Register the listener
        MinecraftServer.getGlobalEventHandler().addListener(EntityDismountEvent.class, new SitDismountListener());

        // Make the player sit
        Pos blockPos = new Pos(0, 0, 0);
        SitHelper.sitPlayer(player, blockPos, appConfig);

        // Verify that the player is sitting
        Assertions.assertTrue(SitHelper.isSitting(player));

        // Get the arrow entity UUID from the player's tag
        UUID arrowUuid = player.getTag(Tags.SIT_ARROW);

        // Get the arrow entity from the instance
        Entity arrowEntity = player.getInstance().getEntityByUuid(arrowUuid);
        Assertions.assertNotNull(arrowEntity, "Arrow entity should not be null");

        // Trigger a dismount event
        EntityDismountEvent dismountEvent = new EntityDismountEvent(player, arrowEntity);
        MinecraftServer.getGlobalEventHandler().call(dismountEvent);

        // Verify that the player is no longer sitting
        Assertions.assertFalse(SitHelper.isSitting(player));
    }
}