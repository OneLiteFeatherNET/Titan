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

/** A horizontal unit direction: where the ascent should lead. */
record Heading(double x, double z) {

    /** Closer to the spawn than this, the offset is noise and the facing direction decides. */
    private static final double MIN_OFFSET = 0.1;

    private static final Heading DEFAULT = new Heading(0.0, 1.0);

    /** From the spawn through the player; on the spawn, the way the player faces. */
    static Heading away(double playerX, double playerZ, double spawnX, double spawnZ, double facingX, double facingZ) {
        double offsetX = playerX - spawnX;
        double offsetZ = playerZ - spawnZ;
        if (Math.hypot(offsetX, offsetZ) >= MIN_OFFSET) {
            return normalized(offsetX, offsetZ);
        }
        return Math.hypot(facingX, facingZ) > 0.0 ? normalized(facingX, facingZ) : DEFAULT;
    }

    /** Cosine between this heading and a step of {@code (dx, dz)} blocks. */
    double dot(int dx, int dz) {
        double length = Math.hypot(dx, dz);
        return (x * dx + z * dz) / length;
    }

    private static Heading normalized(double x, double z) {
        double length = Math.hypot(x, z);
        return new Heading(x / length, z / length);
    }
}
