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

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class HiddenDisplayTest {

    /** Far from every loaded chunk, so placing the display has to wait for the chunk. */
    private static final Pos UNLOADED = new Pos(4000.5, 40.0, 4000.5);

    @Test
    void aDisplayRemovedBeforeItsChunkLoadedLeavesNothingBehind(Env env) {
        Instance instance = env.createFlatInstance();
        Player runner = env.createConnection().connect(instance, new Pos(0.5, 40.0, 0.5));
        HiddenDisplay display = HiddenDisplay.spawn(runner, EntityType.BLOCK_DISPLAY, BlockDisplayMeta.class, meta -> meta.setBlockState(Block.STONE), instance, UNLOADED);

        display.remove().join();

        assertTrue(instance.getEntities().stream().noneMatch(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY), "no display is registered after the chunk arrived");
        assertTrue(display.entity().isRemoved(), "the display is removed");
    }
}
