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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.jumprun.course.TestBlocks;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** A tower falls, lands and rises as one: a display per cell, moved together. */
@ExtendWith(MicrotusExtension.class)
class AnimatedBlockTowerTest {

    private static final BlockPos AT = new BlockPos(0, 45, 0);
    private static final int HEIGHT_OF_TOWER = 3;

    private final Object lock = new Object();
    private final FakeBlocks fakeBlocks = new FakeBlocks();

    private static void tick(Env env, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            env.tick();
        }
    }

    private static double heightOf(Entity display) {
        return ((BlockDisplayMeta) display.getEntityMeta()).getTranslation().y();
    }

    private static List<Entity> blockDisplays(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY).toList();
    }

    @Test
    void aTowerShowsOneDisplayPerCell(Env env) {
        Instance instance = env.createFlatInstance();
        Player runner = env.createPlayer(instance, new Pos(0, 40, 0));

        AnimatedBlock.fallIn(runner, lock, fakeBlocks, TestBlocks.tower(AT, HEIGHT_OF_TOWER), instance, block -> {
        });
        tick(env, 2);

        assertEquals(2 * HEIGHT_OF_TOWER, blockDisplays(instance).size(), "the block plus its 2H - 1 attachments");
    }

    @Test
    void theDisplaysOfATowerFallAtTheSameHeight(Env env) {
        Instance instance = env.createFlatInstance();
        Player runner = env.createPlayer(instance, new Pos(0, 40, 0));
        AnimatedBlock.fallIn(runner, lock, fakeBlocks, TestBlocks.tower(AT, HEIGHT_OF_TOWER), instance, block -> {
        });

        tick(env, 1 + AnimatedBlock.ANIMATION_TICKS / 2);

        List<Double> heights = blockDisplays(instance).stream().map(AnimatedBlockTowerTest::heightOf).distinct().toList();

        assertEquals(1, heights.size(), "all cells are at one height, but were " + heights);
        assertEquals(0.0, heights.getFirst(), 1e-9, "they all fall to the cell of the block, the client interpolates the move");
    }

    @Test
    void aTowerLeavesNoDisplayBehindWhenItIsRemoved(Env env) {
        Instance instance = env.createFlatInstance();
        Player runner = env.createPlayer(instance, new Pos(0, 40, 0));
        AnimatedBlock animated = AnimatedBlock.fallIn(runner, lock, fakeBlocks, TestBlocks.tower(AT, HEIGHT_OF_TOWER), instance, block -> {
        });
        tick(env, 2);

        synchronized (lock) {
            animated.remove();
        }
        tick(env, 2);

        assertEquals(0, blockDisplays(instance).size(), "no display is left after the removal");
    }
}
