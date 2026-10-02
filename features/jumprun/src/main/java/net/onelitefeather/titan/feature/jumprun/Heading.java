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

/** A horizontal unit direction: where a course should lead. */
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

    /** Share of the old heading that survives a step; the rest comes from the step. */
    private static final double KEEP = 0.8;

    /** Cosine between this heading and a step in the direction. */
    double dot(Direction direction) {
        return (x * direction.dx() + z * direction.dz()) / direction.length();
    }

    /** The heading after a step: mostly this one, bent a little towards the step. */
    Heading steered(Direction step) {
        double stepX = step.dx() / step.length();
        double stepZ = step.dz() / step.length();
        return normalized(KEEP * x + (1.0 - KEEP) * stepX, KEEP * z + (1.0 - KEEP) * stepZ);
    }

    /** Share of the old heading that survives a pull towards the wish of a scored course. */
    private static final double KEEP_AGAINST_WISH = 0.75;

    /** The heading after a pull: mostly this one, bent a quarter towards the wish. */
    Heading pulledTowards(Heading wish) {
        return normalized(KEEP_AGAINST_WISH * x + (1.0 - KEEP_AGAINST_WISH) * wish.x, KEEP_AGAINST_WISH * z + (1.0 - KEEP_AGAINST_WISH) * wish.z);
    }

    private static Heading normalized(double x, double z) {
        double length = Math.hypot(x, z);
        return new Heading(x / length, z / length);
    }
}
