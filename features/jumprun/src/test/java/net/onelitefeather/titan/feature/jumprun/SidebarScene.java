/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.jumprun;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;

/**
 * A named player who starts a run in a given mode, with every packet since the start of the run
 * collected, so a test can read what the sidebar was sent.
 */
record SidebarScene(StartedRun run, Collector<ServerPacket> sent) {

    /**
     * @param beforeStart sets the player up while online, before the item is used, for example by
     *                    storing a record
     */
    static SidebarScene start(Env env, JumprunFixture fixture, Instance instance, UUID id, String name, Mode mode, Consumer<Player> beforeStart) {
        TestConnection connection = env.createConnection(new GameProfile(id, name));
        Player player = connection.connect(instance, StartedRun.STAND);
        player.refreshOnGround(true);
        beforeStart.accept(player);
        while (fixture.module().modeOf(player) != mode) {
            player.setSneaking(true);
            fixture.useItem(player);
            player.setSneaking(false);
        }
        Collector<ServerPacket> sent = connection.trackIncoming();
        return new SidebarScene(StartedRun.startAgain(fixture, instance, connection, player), sent);
    }

    Player player() {
        return run.player();
    }

    /** Everything sent since the start of the run; call once, it stops collecting. */
    List<ServerPacket> collect() {
        return sent.collect();
    }
}
