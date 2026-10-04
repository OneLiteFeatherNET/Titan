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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The shapes that show only their block state: stairs with a facing, snow with three layers. */
@ExtendWith(MicrotusExtension.class)
class SurfaceDisplayTest {

    private static final BlockPos AT = new BlockPos(0, 45, 0);
    private static final Pos RUNNER_STAND = new Pos(0.5, 40, 0.5);

    private final FakeBlocks fakeBlocks = new FakeBlocks();

    private static CourseBlock eastStairs() {
        Block material = Surface.STAIRS.shape(Block.OAK_STAIRS).withProperty("facing", "east");
        return new CourseBlock(AT, Surface.STAIRS, material);
    }

    private static CourseBlock snow() {
        return new CourseBlock(AT, Surface.SNOW, Surface.SNOW.shape(Block.SNOW));
    }

    private static BlockDisplayMeta theBlockDisplay(Instance instance) {
        List<BlockDisplayMeta> metas = instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY).map(entity -> (BlockDisplayMeta) entity.getEntityMeta()).toList();
        assertEquals(1, metas.size(), "one block display");
        return metas.getFirst();
    }

    @Test
    void theBlockChangeOfStairsCarriesTheFacing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, RUNNER_STAND);
        Collector<BlockChangePacket> sent = connection.trackIncoming(BlockChangePacket.class);

        fakeBlocks.show(target, List.of(eastStairs()));

        Block shown = Block.fromStateId(sent.collect().getFirst().blockStateId());
        assertEquals("east", shown.getProperty("facing"), "facing");
        assertEquals("bottom", shown.getProperty("half"), "lower half");
    }

    @Test
    void theBlockChangeOfSnowCarriesThreeLayers(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, RUNNER_STAND);
        Collector<BlockChangePacket> sent = connection.trackIncoming(BlockChangePacket.class);

        fakeBlocks.show(target, List.of(snow()));

        assertEquals("3", Block.fromStateId(sent.collect().getFirst().blockStateId()).getProperty("layers"), "layers");
    }

    @Test
    void theFallingDisplayCarriesTheMaterialOfTheBlock(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);

        AnimatedBlock.fallIn(runner, new Object(), fakeBlocks, eastStairs(), instance, landed -> {
        });

        assertEquals(eastStairs().material(), theBlockDisplay(instance).getBlockStateId(), "the same state, facing included");
    }

    @Test
    void theOutlineCarriesTheMaterialOfTheBlock(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);

        new Outline(runner).moveTo(snow(), true);

        assertEquals(snow().material(), theBlockDisplay(instance).getBlockStateId(), "the same state, layers included");
    }

    @Test
    void aRainbowChangeOfStairsShowsTheNewMaterialWithTheSameFacing(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);
        CourseGenerator generator = TestBlocks.generator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, RandomGeneratorFactory.of("L64X128MixRandom").create(3L));
        AnimatedBlock animated = AnimatedBlock.fallIn(runner, new Object(), fakeBlocks, eastStairs(), instance, landed -> {
        });
        for (int tick = 0; tick <= AnimatedBlock.ANIMATION_TICKS + 2; tick++) {
            env.tick();
        }
        CourseBlock redrawn = generator.redrawn(eastStairs());

        animated.recolor(redrawn);

        Block shown = theBlockDisplay(instance).getBlockStateId();
        assertEquals("east", shown.getProperty("facing"), "facing kept");
        assertEquals(redrawn.material(), shown, "the new material");
    }
}
