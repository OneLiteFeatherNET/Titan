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

import net.minestom.server.instance.block.Block;

/** One block of a course: where it sits, its shape and the material drawn for it. */
record CourseBlock(BlockPos pos, Surface surface, Block material) {

    /** With the first material of the shape; for blocks whose look does not matter. */
    CourseBlock(BlockPos pos, Surface surface) {
        this(pos, surface, surface.palette().getFirst());
    }

    CourseBlock withMaterial(Block material) {
        return new CourseBlock(pos, surface, material);
    }

    /** Absolute y of the walkable top. */
    double topY() {
        return pos.y() + surface.top();
    }

    /** The highest y a standing player reaches into above this block. */
    int headroomTopY() {
        return pos.y() + surface.headroomTop();
    }
}
