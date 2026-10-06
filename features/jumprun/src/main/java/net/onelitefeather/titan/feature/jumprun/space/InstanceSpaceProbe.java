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

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;

/** {@link SpaceProbe} over a live instance. */
public record InstanceSpaceProbe(Instance instance) implements SpaceProbe {

    /** An unloaded chunk counts as occupied: a block there could not be shown or checked. */
    @Override
    public boolean isAir(BlockPos pos) {
        if (!instance.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
            return false;
        }
        return instance.getBlock(pos.x(), pos.y(), pos.z(), Block.Getter.Condition.TYPE).air();
    }

    @Override
    public boolean inBounds(BlockPos pos) {
        DimensionType dimension = instance.getCachedDimensionType();
        boolean insideDimension = pos.y() >= dimension.minY() && pos.y() < dimension.maxY();
        return insideDimension && instance.getWorldBorder().inBounds(new Vec(pos.x() + 0.5, pos.y(), pos.z() + 0.5));
    }
}
