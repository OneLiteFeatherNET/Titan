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

/** A place a course block could go: position and shape, before a material is drawn for it. */
record Spot(BlockPos pos, Surface surface) implements Placement {

    CourseBlock withMaterial(Block material) {
        return new CourseBlock(pos, surface, material);
    }
}
