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
package net.onelitefeather.titan.feature.jumprun.course;

import java.util.List;
import java.util.Optional;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;

/** Where a block of a course sits and what shape it has; what the jump rules need to know. */
sealed interface Placement permits Spot, CourseBlock {

    BlockPos pos();

    Surface surface();

    /** The climbing tower this block is the top of, if any. */
    Optional<Climb> climb();

    /** The blocks of a tower beside this one: its pillar and its ladder or vine. */
    default List<Cell> attachments() {
        return List.of();
    }

    /** Absolute y of the walkable top. */
    default double topY() {
        return pos().y() + surface().top();
    }

    /** Absolute y of the lowest step: where a runner may take off from or fall past. */
    default double lowTopY() {
        return pos().y() + surface().lowTop();
    }

    /** The highest y a player reaches into at the apex of a jump from this block. */
    default int jumpRoomTopY() {
        return pos().y() + surface().jumpRoomTop();
    }
}
