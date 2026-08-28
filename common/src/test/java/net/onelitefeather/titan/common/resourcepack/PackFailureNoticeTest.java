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
package net.onelitefeather.titan.common.resourcepack;

import net.kyori.adventure.resource.ResourcePackStatus;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class PackFailureNoticeTest {

    @Test
    @DisplayName("A pack that could not be downloaded or loaded is reported to the player")
    void testFailureIsReported(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        PackFailureNotice notice = new PackFailureNotice(PackSlot.SEASON);
        notice.handleStatus(player, ResourcePackStatus.FAILED_DOWNLOAD);
        notice.handleStatus(player, ResourcePackStatus.INVALID_URL);
        notice.handleStatus(player, ResourcePackStatus.FAILED_RELOAD);

        messages.assertCount(3);
    }

    @Test
    @DisplayName("A loaded pack and a declined pack are not worth a message")
    void testSilentStatuses(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        PackFailureNotice notice = new PackFailureNotice(PackSlot.BASE);
        notice.handleStatus(player, ResourcePackStatus.SUCCESSFULLY_LOADED);
        notice.handleStatus(player, ResourcePackStatus.DOWNLOADED);
        // The player chose this, and for a required pack Minestom has already kicked them.
        notice.handleStatus(player, ResourcePackStatus.DECLINED);

        messages.assertEmpty();
    }
}
