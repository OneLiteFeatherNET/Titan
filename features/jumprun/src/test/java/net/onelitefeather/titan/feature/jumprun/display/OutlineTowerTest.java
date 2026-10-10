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

/** The glowing outline of a tower marks its target block only, never the ladder or the pillar. */
@ExtendWith(MicrotusExtension.class)
class OutlineTowerTest {

    private static final BlockPos TARGET = new BlockPos(0, 45, 0);
    private static final int HEIGHT_OF_TOWER = 3;

    private static List<Entity> glowingDisplays(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY).filter(entity -> ((BlockDisplayMeta) entity.getEntityMeta()).isHasGlowingEffect()).toList();
    }

    @Test
    void theOutlineOfATowerMarksOnlyItsTargetBlock(Env env) {
        Instance instance = env.createFlatInstance();
        Player runner = env.createPlayer(instance, new Pos(0, 40, 0));
        Outline outline = new Outline(runner);

        outline.moveTo(TestBlocks.tower(TARGET, HEIGHT_OF_TOWER), true);
        env.tick();

        List<Entity> glowing = glowingDisplays(instance);
        assertEquals(1, glowing.size(), "one outline, for the target block");
        assertEquals(TARGET.y(), glowing.getFirst().getPosition().blockY(), "the outline sits on the target, not on the ladder below it");
    }

    @Test
    void theOutlineOfATowerLeavesNoDisplayBehindWhenItIsRemoved(Env env) {
        Instance instance = env.createFlatInstance();
        Player runner = env.createPlayer(instance, new Pos(0, 40, 0));
        Outline outline = new Outline(runner);
        outline.moveTo(TestBlocks.tower(TARGET, HEIGHT_OF_TOWER), true);
        env.tick();

        outline.remove();
        env.tick();

        assertEquals(0, glowingDisplays(instance).size(), "no outline is left after the removal");
    }
}
