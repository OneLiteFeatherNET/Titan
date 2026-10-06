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

import java.util.Collection;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockEntityType;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.player.ResolvableProfile;
import net.onelitefeather.titan.feature.jumprun.course.CourseBlock;
import net.onelitefeather.titan.feature.jumprun.head.HeadSkin;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;

/** Shows blocks to one player only; the instance itself is never changed. */
public final class FakeBlocks {

    public void show(Player player, Collection<CourseBlock> blocks) {
        for (CourseBlock block : blocks) {
            player.sendPacket(new BlockChangePacket(toPoint(block.pos()), block.material()));
            block.skin().ifPresent(skin -> player.sendPacket(new BlockEntityDataPacket(toPoint(block.pos()), BlockEntityType.SKULL, skullData(skin))));
        }
    }

    /** The profile is the whole data of a skull the client has to show with a skin. */
    private static CompoundBinaryTag skullData(HeadSkin skin) {
        return CompoundBinaryTag.builder().put("profile", ResolvableProfile.CODEC.encode(Transcoder.NBT, skin.profile()).orElseThrow()).build();
    }

    /** Sends the real block of the instance back, so the player sees the world again. */
    public void reset(Player player, Collection<CourseBlock> blocks) {
        Instance instance = player.getInstance();
        if (instance == null) {
            return;
        }
        for (CourseBlock block : blocks) {
            BlockPos pos = block.pos();
            if (instance.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
                player.sendPacket(new BlockChangePacket(toPoint(pos), realBlock(instance, pos)));
            }
        }
    }

    private static Block realBlock(Instance instance, BlockPos pos) {
        return instance.getBlock(pos.x(), pos.y(), pos.z());
    }

    private static BlockVec toPoint(BlockPos pos) {
        return new BlockVec(pos.x(), pos.y(), pos.z());
    }
}
