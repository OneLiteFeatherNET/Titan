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
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;

/**
 * One block of the window as a display that falls into place and rises away again. The runner
 * gets the real fake block when the fall is over, so nothing is solid that still moves.
 *
 * <p>The steps run on the tick of the display, so each one takes the lock of the run first and
 * drops out when the block has moved on to another state meanwhile, which makes a step that
 * outlives its block harmless. A removed display never ticks again, so none is left behind.
 */
final class AnimatedBlock {

    /** Under half a second, as the spec asks. */
    static final int ANIMATION_TICKS = 8;

    private static final Vec HEIGHT = new Vec(0, 6, 0);
    private static final int NO_START_DELAY = 0;

    private enum State {
        FALLING, LANDED, RISING, REMOVED
    }

    private final Player runner;
    private final Object lock;
    private final FakeBlocks fakeBlocks;
    private final CourseBlock block;
    private final HiddenDisplay display;
    private State state = State.FALLING;

    private AnimatedBlock(Player runner, Object lock, FakeBlocks fakeBlocks, CourseBlock block, Instance instance) {
        this.runner = runner;
        this.lock = lock;
        this.fakeBlocks = fakeBlocks;
        this.block = block;
        BlockPos pos = block.pos();
        this.display = HiddenDisplay.spawn(runner, EntityType.BLOCK_DISPLAY, BlockDisplayMeta.class, meta -> {
            meta.setBlockState(block.material());
            meta.setTranslation(HEIGHT);
            meta.setTransformationInterpolationStartDelta(NO_START_DELAY);
        }, instance, new Pos(pos.x(), pos.y(), pos.z()), true);
    }

    /**
     * Spawns the block high up for everyone, runner included, and lets it fall from the next tick
     * on, so the client has the start position before it moves.
     */
    static AnimatedBlock fallIn(Player runner, Object lock, FakeBlocks fakeBlocks, CourseBlock block, Instance instance) {
        AnimatedBlock animated = new AnimatedBlock(runner, lock, fakeBlocks, block, instance);
        animated.after(1, animated::startFall);
        return animated;
    }

    /** Whether the runner has the block for real; the caller holds the lock of the run. */
    boolean isLanded() {
        return state == State.LANDED;
    }

    /**
     * Starts the rise for everyone, runner included; the runner's real block is the caller's
     * business. The caller holds the lock of the run.
     */
    void riseAway() {
        if (state == State.RISING || state == State.REMOVED) {
            return;
        }
        state = State.RISING;
        display.showToRunner(true);
        display.entity().editEntityMeta(BlockDisplayMeta.class, meta -> animateTo(meta, HEIGHT));
        after(ANIMATION_TICKS, this::remove);
    }

    /** Removes the display at once, without animation; the caller holds the lock of the run. */
    void remove() {
        state = State.REMOVED;
        display.remove();
    }

    private void startFall() {
        if (state != State.FALLING) {
            return;
        }
        display.entity().editEntityMeta(BlockDisplayMeta.class, meta -> animateTo(meta, Vec.ZERO));
        after(ANIMATION_TICKS, this::land);
    }

    private void land() {
        if (state != State.FALLING) {
            return;
        }
        state = State.LANDED;
        fakeBlocks.show(runner, List.of(block));
        display.showToRunner(false);
    }

    /** Fall and rise share this move, so the rise is the fall backwards; the size never changes. */
    private static void animateTo(BlockDisplayMeta meta, Vec translation) {
        meta.setTransformationInterpolationDuration(ANIMATION_TICKS);
        meta.setTransformationInterpolationStartDelta(NO_START_DELAY);
        meta.setTranslation(translation);
    }

    private void after(int ticks, Runnable step) {
        display.entity().scheduler().buildTask(() -> {
            synchronized (lock) {
                step.run();
            }
        }).delay(TaskSchedule.tick(ticks)).schedule();
    }
}
