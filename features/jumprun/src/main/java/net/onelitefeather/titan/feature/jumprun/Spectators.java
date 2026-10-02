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
import java.util.List;
import java.util.Map;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

/**
 * The blocks of one run as everyone but the runner sees them: a block display per block, which
 * has no collision, so nobody can stand on it or be blocked by it. A new block falls into place
 * and only then is real for the runner, a removed one rises away; see {@link AnimatedBlock}.
 *
 * <p>Not thread-safe by itself: the caller holds the lock of the run, like for the fake blocks.
 */
final class Spectators {

    private final Player runner;
    private final Object lock;
    private final FakeBlocks fakeBlocks;
    private final Map<BlockPos, AnimatedBlock> blocks = new HashMap<>();

    /**
     * @param lock what the animation steps synchronize on, the run that owns these spectators
     */
    Spectators(Player runner, Object lock, FakeBlocks fakeBlocks) {
        this.runner = runner;
        this.lock = lock;
        this.fakeBlocks = fakeBlocks;
    }

    /** Lets the blocks fall in; the runner gets each one for real when it has landed. */
    void show(Collection<CourseBlock> shown) {
        Instance instance = runner.getInstance();
        if (instance == null) {
            return;
        }
        for (CourseBlock block : shown) {
            AnimatedBlock previous = blocks.put(block.pos(), AnimatedBlock.fallIn(runner, lock, fakeBlocks, block, instance));
            if (previous != null) {
                previous.remove();
            }
        }
    }

    /** Lets the blocks rise away; the real block for the runner is the caller's business. */
    void hide(Collection<CourseBlock> hidden) {
        for (CourseBlock block : hidden) {
            AnimatedBlock animated = blocks.remove(block.pos());
            if (animated != null) {
                animated.riseAway();
            }
        }
    }

    /** Lets every remaining block rise away, because the run is over. */
    void riseAll() {
        blocks.values().forEach(AnimatedBlock::riseAway);
        blocks.clear();
    }

    /** Removes every display of the run at once. */
    void clear() {
        blocks.values().forEach(AnimatedBlock::remove);
        blocks.clear();
    }

    /** The blocks of the given ones that the runner can stand on by now. */
    List<CourseBlock> landed(Collection<CourseBlock> candidates) {
        return candidates.stream().filter(block -> {
            AnimatedBlock animated = blocks.get(block.pos());
            return animated != null && animated.isLanded();
        }).toList();
    }
}
