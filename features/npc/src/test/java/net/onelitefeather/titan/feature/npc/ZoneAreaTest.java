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
package net.onelitefeather.titan.feature.npc;

import java.util.Random;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZoneAreaTest {

    private static final Box TUNNEL = new Box(new Vec(82, 78, 152), new Vec(123, 90, 163));

    @DisplayName("A point on the face of the box is inside")
    @Test
    void faceIsInside() {
        Assertions.assertTrue(ZoneArea.inside(TUNNEL, new Pos(82, 78, 152)), "the faces belong to the zone");
    }

    @DisplayName("A point just outside the box is not inside")
    @Test
    void justOutsideIsNotInside() {
        Assertions.assertFalse(ZoneArea.inside(TUNNEL, new Pos(81.9, 80, 157)), "a point 0.1 blocks west of the zone is outside");
    }

    @DisplayName("A point up to the margin outside the box is near, further is not")
    @Test
    void nearUsesTheMargin() {
        Assertions.assertTrue(ZoneArea.near(TUNNEL, new Pos(76, 80, 157), 6), "six blocks west is still near");
        Assertions.assertFalse(ZoneArea.near(TUNNEL, new Pos(75, 80, 157), 6), "seven blocks west is not near");
    }

    @DisplayName("A random point lies inside the box")
    @Test
    void randomPointIsInside() {
        Random random = new Random(7);
        for (int i = 0; i < 50; i++) {
            Vec point = ZoneArea.randomPoint(TUNNEL, random);
            Assertions.assertTrue(ZoneArea.inside(TUNNEL, point), "every random point must be inside: " + point);
        }
    }
}
