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

/** Where a block of a course sits and what shape it has; what the jump rules need to know. */
sealed interface Placement permits Spot, CourseBlock {

    BlockPos pos();

    Surface surface();

    /** Absolute y of the walkable top. */
    default double topY() {
        return pos().y() + surface().top();
    }

    /** The highest y a player reaches into at the apex of a jump from this block. */
    default int jumpRoomTopY() {
        return pos().y() + surface().jumpRoomTop();
    }
}
