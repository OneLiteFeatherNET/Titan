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
package net.onelitefeather.titan.feature.jumprun.space;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class InstanceSpaceProbeTest {

    @Test
    void airAboveTheGroundIsAir(Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();

        assertTrue(new InstanceSpaceProbe(instance).isAir(new BlockPos(0, 60, 0)));
    }

    @Test
    void aSolidBlockIsNotAir(Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();
        instance.setBlock(0, 60, 0, Block.STONE);

        assertFalse(new InstanceSpaceProbe(instance).isAir(new BlockPos(0, 60, 0)));
    }

    @Test
    void anUnloadedChunkIsNotAir(Env env) {
        Instance instance = env.createFlatInstance();

        assertFalse(new InstanceSpaceProbe(instance).isAir(new BlockPos(160_000, 60, 160_000)));
    }

    @Test
    void aPositionInsideTheDimensionIsInBounds(Env env) {
        Instance instance = env.createFlatInstance();

        assertTrue(new InstanceSpaceProbe(instance).inBounds(new BlockPos(0, 60, 0)));
    }

    @Test
    void belowTheDimensionIsOutOfBounds(Env env) {
        Instance instance = env.createFlatInstance();
        int minY = instance.getCachedDimensionType().minY();

        assertFalse(new InstanceSpaceProbe(instance).inBounds(new BlockPos(0, minY - 1, 0)));
    }

    @Test
    void aboveTheDimensionIsOutOfBounds(Env env) {
        Instance instance = env.createFlatInstance();
        int maxY = instance.getCachedDimensionType().maxY();

        assertFalse(new InstanceSpaceProbe(instance).inBounds(new BlockPos(0, maxY, 0)));
    }

    @Test
    void outsideTheWorldBorderIsOutOfBounds(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setWorldBorder(instance.getWorldBorder().withDiameter(20));

        assertFalse(new InstanceSpaceProbe(instance).inBounds(new BlockPos(100, 60, 0)));
        assertTrue(new InstanceSpaceProbe(instance).inBounds(new BlockPos(5, 60, 0)));
    }
}
