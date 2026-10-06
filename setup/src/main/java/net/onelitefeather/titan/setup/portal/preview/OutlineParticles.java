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

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

import java.util.List;

/** Sends outline points to one player as particles; nobody else gets a packet. */
final class OutlineParticles {

    /** Both the show and the preview repeat at this rate. */
    static final int INTERVAL_TICKS = 5;

    private OutlineParticles() {
    }

    static void send(Player player, List<? extends Point> points) {
        for (Point point : points) {
            // longDistance so builders see the outline of a big ring from across the map.
            player.sendPacket(new ParticlePacket(Particle.END_ROD, true, true, point.x(), point.y(), point.z(), 0, 0, 0, 0, 1));
        }
    }
}
