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
import java.util.HashMap;
import java.util.Map;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;

/**
 * The blocks of one run as everyone but the runner sees them: a block display per block, which
 * has no collision, so nobody can stand on it or be blocked by it. The runner has the real
 * packets of {@link FakeBlocks} instead.
 *
 * <p>Not thread-safe by itself: the caller holds the lock of the run, like for the fake blocks.
 */
final class Spectators {

    private final Player runner;
    private final Map<BlockPos, HiddenDisplay> displays = new HashMap<>();

    Spectators(Player runner) {
        this.runner = runner;
    }

    void show(Collection<CourseBlock> blocks) {
        Instance instance = runner.getInstance();
        if (instance == null) {
            return;
        }
        for (CourseBlock block : blocks) {
            HiddenDisplay previous = displays.put(block.pos(), spawn(instance, block));
            if (previous != null) {
                previous.remove();
            }
        }
    }

    void hide(Collection<CourseBlock> blocks) {
        for (CourseBlock block : blocks) {
            HiddenDisplay display = displays.remove(block.pos());
            if (display != null) {
                display.remove();
            }
        }
    }

    /** Removes every display of the run. */
    void clear() {
        displays.values().forEach(HiddenDisplay::remove);
        displays.clear();
    }

    private HiddenDisplay spawn(Instance instance, CourseBlock block) {
        BlockPos pos = block.pos();
        return HiddenDisplay.spawn(runner, EntityType.BLOCK_DISPLAY, BlockDisplayMeta.class, meta -> meta.setBlockState(block.material()), instance, new Pos(pos.x(), pos.y(), pos.z()));
    }
}
