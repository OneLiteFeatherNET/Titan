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
import java.util.Optional;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;

/**
 * One block of a course: where it sits, its shape and the material drawn for it. A head may show
 * the skin of a team member, which is not part of the block state.
 */
record CourseBlock(BlockPos pos, Surface surface, Block material,
                   Optional<HeadSkin> skin) implements Placement {

    /**
     * A landing reported by the client is a hair off the exact top; this much is still "standing on
     * it".
     */
    private static final double LANDING_TOLERANCE = 0.05;

    /** A player standing on an edge still has the step under part of the hitbox. */
    private static final double PLAYER_HALF_WIDTH = 0.3;

    CourseBlock(BlockPos pos, Surface surface, Block material) {
        this(pos, surface, material, Optional.empty());
    }

    List<Step> steps() {
        return surface.steps(material);
    }

    /** Whether feet at {@code feet} stand on one of the steps, edge included. */
    boolean supports(Point feet) {
        return steps().stream().anyMatch(step -> isOn(step, feet));
    }

    /**
     * The same half width serves every shape: the height check gates it, and being lenient on thin
     * posts and panes, whose step is the whole cell, is intended.
     */
    private boolean isOn(Step step, Point feet) {
        boolean atHeight = Math.abs(feet.y() - (pos.y() + step.top())) <= LANDING_TOLERANCE;
        return atHeight && reaches(feet.x(), pos.x(), step.minX(), step.maxX()) && reaches(feet.z(), pos.z(), step.minZ(), step.maxZ());
    }

    private static boolean reaches(double center, int cell, double min, double max) {
        return center >= cell + min - PLAYER_HALF_WIDTH && center <= cell + max + PLAYER_HALF_WIDTH;
    }
}
