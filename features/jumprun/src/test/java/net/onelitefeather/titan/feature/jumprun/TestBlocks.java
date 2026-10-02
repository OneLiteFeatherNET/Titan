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

/** Fixture factory for blocks whose material does not matter to the test. */
final class TestBlocks {

    private TestBlocks() {
    }

    /** A block with the first material of its shape. */
    static CourseBlock at(BlockPos pos, Surface surface) {
        return new CourseBlock(pos, surface, surface.palette().getFirst());
    }
}
