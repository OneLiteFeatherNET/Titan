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
package net.onelitefeather.titan.feature.jumprun.display;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.feature.jumprun.course.CourseBlock;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;

/**
 * The glowing outline around the block the runner has to reach next: a slightly larger display of
 * the same block that only the runner sees. It shows up once that block has landed, so it never
 * hangs around an empty place, and moves on with every landing.
 *
 * <p>Not thread-safe by itself: the caller holds the lock of the run, like for {@link Spectators}.
 */
final class Outline {

    /** Matches the green of the title. */
    private static final int GLOW_COLOR = 0x7CFC00;

    private static final int FULL_LIGHT = 15;

    private final Player runner;
    private CourseBlock target;
    private HiddenDisplay display;

    Outline(Player runner) {
        this.runner = runner;
    }

    /** Takes the outline from the old block and puts it on the next one, if that has landed. */
    void moveTo(CourseBlock next, boolean landed) {
        remove();
        target = next;
        if (landed) {
            spawn();
        }
    }

    /** Called when a block of the run has landed; outlines it if it is the one waited for. */
    void blockLanded(CourseBlock block) {
        if (display == null && target != null && target.pos().equals(block.pos())) {
            spawn();
        }
    }

    /** Removes the outline at once, and none appears after this. */
    void remove() {
        target = null;
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    private void spawn() {
        Instance instance = runner.getInstance();
        if (instance == null) {
            return;
        }
        CourseBlock block = target;
        BlockPos pos = block.pos();
        display = HiddenDisplay.spawnForRunnerOnly(runner, BlockLook.entityType(block), AbstractDisplayMeta.class, meta -> {
            BlockLook.show(meta, block);
            meta.setScale(BlockLook.outlineScale(block));
            meta.setTranslation(BlockLook.outlineTranslation(block));
            meta.setHasGlowingEffect(true);
            meta.setGlowColorOverride(GLOW_COLOR);
            // The display sits inside the opaque course block, where the sampled light is 0 and it renders black.
            meta.setBrightness(FULL_LIGHT, FULL_LIGHT);
        }, instance, new Pos(pos.x(), pos.y(), pos.z()));
    }
}
