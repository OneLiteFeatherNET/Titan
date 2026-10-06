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
package net.onelitefeather.titan.setup.portal.preview;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.editor.PortalOutline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PortalShowTest {

    private static final Portal PORTAL = new Portal("p", new Box(new Vec(0, 64, 0), new Vec(2, 66, 2)), "Task", null);
    private static final int POINTS = PortalOutline.points(PORTAL.shape()).size();

    private final PortalShow show = new PortalShow();

    @DisplayName("Nothing is started for a world without portals")
    @Test
    void emptyWorldStartsNothing(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), new Pos(0, 40, 0));

        assertFalse(show.start(player, List.of()), "an empty world has nothing to show");
        assertEquals(0, show.running(), "no task for an empty world");
    }

    @DisplayName("The outline goes to the executing player only, one batch every 5 ticks for 32 runs")
    @Test
    void outlineGoesOnlyToTheExecutingPlayerFor32Runs(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection viewerConnection = env.createConnection();
        Player viewer = viewerConnection.connect(instance, new Pos(0, 40, 0));
        Collector<ParticlePacket> mine = viewerConnection.trackIncoming(ParticlePacket.class);
        TestConnection otherConnection = env.createConnection();
        otherConnection.connect(instance, new Pos(0, 40, 0));
        Collector<ParticlePacket> others = otherConnection.trackIncoming(ParticlePacket.class);

        assertTrue(show.start(viewer, List.of(PORTAL)), "a portal exists, so the show starts");
        tick(env, 200);

        assertEquals(32 * POINTS, mine.collect().size(), "32 runs of the full outline");
        assertTrue(others.collect().isEmpty(), "nobody else sees the outline");
        assertEquals(0, show.running(), "the task ends after its 32 runs");
    }

    @DisplayName("A second show replaces the running one instead of doubling it")
    @Test
    void secondShowReplacesTheFirst(Env env) {
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance(), new Pos(0, 40, 0));
        show.start(player, List.of(PORTAL));
        tick(env, 12);

        // A collector stops tracking once collected, so the second phase gets a fresh one.
        Collector<ParticlePacket> packets = connection.trackIncoming(ParticlePacket.class);
        show.start(player, List.of(PORTAL));
        assertEquals(1, show.running(), "one task per player");
        tick(env, 200);

        assertEquals(32 * POINTS, packets.collect().size(), "the replacement runs its own 32 runs and the first sends no more");
    }

    @DisplayName("Stopping ends the show at once")
    @Test
    void stopEndsTheShow(Env env) {
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance(), new Pos(0, 40, 0));
        show.start(player, List.of(PORTAL));
        tick(env, 6);

        Collector<ParticlePacket> packets = connection.trackIncoming(ParticlePacket.class);
        show.stop(player.getUuid());
        tick(env, 50);

        assertTrue(packets.collect().isEmpty(), "no packets after stop");
        assertEquals(0, show.running(), "no task left");
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }
}
