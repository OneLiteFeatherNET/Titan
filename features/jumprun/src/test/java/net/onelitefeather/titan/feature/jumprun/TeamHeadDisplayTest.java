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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockEntityType;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A team head reaches the client three ways: the fake block with a profile, and the falling and the
 * outline display as an item display, because a block display cannot carry a profile.
 */
@ExtendWith(MicrotusExtension.class)
class TeamHeadDisplayTest {

    private static final BlockPos AT = new BlockPos(0, 45, 0);
    private static final Pos RUNNER_STAND = new Pos(0.5, 40, 0.5);
    private static final HeadSkin ALEX = new HeadSkin(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "textures-alex", "signature-alex");
    private static final HeadSkin BOB = new HeadSkin(UUID.fromString("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6"), "textures-bob", "signature-bob");

    private final FakeBlocks fakeBlocks = new FakeBlocks();

    private static CourseBlock teamHead(HeadSkin skin) {
        return new CourseBlock(AT, Surface.HEAD, Block.PLAYER_HEAD.withProperty("rotation", "4"), Optional.of(skin));
    }

    private static CourseBlock plainHead() {
        return new CourseBlock(AT, Surface.HEAD, Block.ZOMBIE_HEAD);
    }

    private static ResolvableProfile profileOf(Entity display) {
        ItemDisplayMeta meta = (ItemDisplayMeta) display.getEntityMeta();
        assertEquals(Material.PLAYER_HEAD, meta.getItemStack().material(), "the display shows a player head");
        return meta.getItemStack().get(DataComponents.PROFILE);
    }

    private static List<Entity> displays(Instance instance, EntityType type) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == type).toList();
    }

    private static ResolvableProfile profileInSkullData(BlockEntityDataPacket packet) {
        BinaryTag profile = packet.data().get("profile");
        return ResolvableProfile.CODEC.decode(Transcoder.NBT, profile).orElseThrow();
    }

    // --- the fake block ------------------------------------------------------------------------------

    @Test
    void aTeamHeadIsFollowedByItsSkullDataWithTheProfile(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, RUNNER_STAND);
        Collector<ServerPacket> sent = connection.trackIncoming();

        fakeBlocks.show(target, List.of(teamHead(ALEX)));

        List<ServerPacket> packets = sent.collect().stream().filter(packet -> packet instanceof BlockChangePacket || packet instanceof BlockEntityDataPacket).toList();
        assertEquals(2, packets.size(), "the block, then its data");
        assertTrue(packets.get(0) instanceof BlockChangePacket, "the block change comes first");
        BlockEntityDataPacket data = (BlockEntityDataPacket) packets.get(1);
        assertEquals(BlockEntityType.SKULL, data.type(), "a skull");
        assertEquals(AT.x(), data.blockPosition().blockX(), "x");
        assertEquals(AT.y(), data.blockPosition().blockY(), "y");
        assertEquals(AT.z(), data.blockPosition().blockZ(), "z");
        assertEquals(ALEX.profile(), profileInSkullData(data), "uuid and texture travel in the profile");
    }

    @Test
    void aPlainHeadAndOtherShapesSendNoBlockEntityData(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, RUNNER_STAND);
        Collector<BlockEntityDataPacket> data = connection.trackIncoming(BlockEntityDataPacket.class);

        fakeBlocks.show(target, List.of(plainHead(), TestBlocks.at(new BlockPos(2, 45, 0), Surface.CANDLE)));

        data.assertEmpty();
    }

    @Test
    void resetOfATeamHeadSendsOnlyTheRealBlock(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, RUNNER_STAND);
        Collector<BlockChangePacket> blocks = connection.trackIncoming(BlockChangePacket.class);
        Collector<BlockEntityDataPacket> data = connection.trackIncoming(BlockEntityDataPacket.class);

        fakeBlocks.reset(target, List.of(teamHead(ALEX)));

        assertEquals(Block.AIR.stateId(), blocks.collect().getFirst().blockStateId(), "the real block replaces the fake one and its entity");
        data.assertEmpty();
    }

    // --- the falling display -------------------------------------------------------------------------

    @Test
    void aFallingTeamHeadIsAnItemDisplayWithItsProfile(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);

        AnimatedBlock.fallIn(runner, new Object(), fakeBlocks, teamHead(ALEX), instance, landed -> {
        });

        assertEquals(List.of(), displays(instance, EntityType.BLOCK_DISPLAY), "no block display, it could not carry the profile");
        List<Entity> items = displays(instance, EntityType.ITEM_DISPLAY);
        assertEquals(1, items.size(), "one item display");
        assertEquals(ALEX.profile(), profileOf(items.getFirst()), "the profile of the head");
    }

    @Test
    void aFallingPlainHeadStaysABlockDisplay(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);

        AnimatedBlock.fallIn(runner, new Object(), fakeBlocks, plainHead(), instance, landed -> {
        });

        assertEquals(List.of(), displays(instance, EntityType.ITEM_DISPLAY), "no item display");
        BlockDisplayMeta meta = (BlockDisplayMeta) displays(instance, EntityType.BLOCK_DISPLAY).getFirst().getEntityMeta();
        assertEquals(Block.ZOMBIE_HEAD.stateId(), meta.getBlockStateId().stateId(), "the block of the head");
    }

    @Test
    void aLandedTeamHeadShowsAnotherSkinAfterARecolor(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);
        AnimatedBlock animated = AnimatedBlock.fallIn(runner, new Object(), fakeBlocks, teamHead(ALEX), instance, landed -> {
        });
        for (int tick = 0; tick <= AnimatedBlock.ANIMATION_TICKS + 2; tick++) {
            env.tick();
        }

        animated.recolor(teamHead(BOB));

        assertEquals(BOB.profile(), profileOf(displays(instance, EntityType.ITEM_DISPLAY).getFirst()), "the display follows the new skin");
    }

    // --- the outline ---------------------------------------------------------------------------------

    @Test
    void theOutlineOfATeamHeadIsAGlowingItemDisplayWithItsProfile(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);

        new Outline(runner).moveTo(teamHead(BOB), true);

        Entity outline = displays(instance, EntityType.ITEM_DISPLAY).getFirst();
        ItemDisplayMeta meta = (ItemDisplayMeta) outline.getEntityMeta();
        assertEquals(BOB.profile(), profileOf(outline), "the profile of the head");
        assertTrue(meta.isHasGlowingEffect(), "glows like every outline");
        assertEquals(List.of(runner), List.copyOf(outline.getViewers()), "the runner alone sees it");
    }

    @Test
    void theOutlineOfOtherShapesStaysABlockDisplay(Env env) {
        Instance instance = JumprunFixture.loadedInstance(env);
        Player runner = env.createConnection().connect(instance, RUNNER_STAND);

        new Outline(runner).moveTo(TestBlocks.at(AT, Surface.CANDLE), true);

        assertEquals(List.of(), displays(instance, EntityType.ITEM_DISPLAY), "no item display");
        assertEquals(1, displays(instance, EntityType.BLOCK_DISPLAY).size(), "a block display");
    }
}
