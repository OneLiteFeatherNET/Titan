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
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;

/** The geometry of an ambience zone: who is inside, who is near, where a particle may appear. */
final class ZoneArea {

    private ZoneArea() {
    }

    /** Whether the point lies in the box, the faces included. */
    static boolean inside(Box box, Point point) {
        return between(box.min().x(), box.max().x(), point.x()) && between(box.min().y(), box.max().y(), point.y()) && between(box.min().z(), box.max().z(), point.z());
    }

    /** Whether the point lies in the box grown by {@code margin} on every side. */
    static boolean near(Box box, Point point, double margin) {
        return inside(new Box(box.min().sub(margin, margin, margin), box.max().add(margin, margin, margin)), point);
    }

    /** A random point inside the box. */
    static Vec randomPoint(Box box, Random random) {
        Vec min = box.min();
        Vec max = box.max();
        return new Vec(min.x() + random.nextDouble() * (max.x() - min.x()), min.y() + random.nextDouble() * (max.y() - min.y()), min.z() + random.nextDouble() * (max.z() - min.z()));
    }

    private static boolean between(double min, double max, double value) {
        return value >= min && value <= max;
    }
}
