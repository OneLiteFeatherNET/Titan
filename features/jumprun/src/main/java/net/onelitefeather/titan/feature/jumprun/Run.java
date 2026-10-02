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

import java.util.Collection;
import java.util.List;
import net.minestom.server.entity.Player;

/**
 * One player's running course. The start block is a real block of the world, so it is never part
 * of what is shown or reset.
 */
record Run(Player player, Course course, BlockPos startBlock) {

    /** The blocks of the visible window that exist only on the player's screen. */
    List<CourseBlock> fakeWindow() {
        return fake(course.window());
    }

    List<CourseBlock> fake(Collection<CourseBlock> blocks) {
        return blocks.stream().filter(block -> !block.pos().equals(startBlock)).toList();
    }

    boolean showsFakeBlockAt(int x, int y, int z) {
        return fakeWindow().stream().anyMatch(block -> block.pos().equals(new BlockPos(x, y, z)));
    }
}
