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

import io.avaje.config.Configuration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionStatusPacket;
import net.minestom.server.network.packet.client.play.ClientTeleportConfirmPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Env;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.course.Surface;
import net.onelitefeather.titan.feature.jumprun.course.TestBlocks;
import net.onelitefeather.titan.feature.jumprun.display.AnimatedBlock;
import net.onelitefeather.titan.feature.jumprun.persistence.EndReason;
import net.onelitefeather.titan.feature.jumprun.persistence.FinishedRun;
import net.onelitefeather.titan.feature.jumprun.persistence.InMemoryRunRecords;
import net.onelitefeather.titan.feature.jumprun.persistence.RunRecords;

/**
 * A started {@link JumprunModule} on a fresh {@code titan} node with a fixed seed, in-memory
 * records and its own message store, torn down again by {@link #close()}.
 */
public final class JumprunFixture implements AutoCloseable {

    static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final long SEED = 42L;

    /** The flat instance is stone up to y 39, so a player stands at y 40. */
    static final double GROUND_Y = 40.0;

    /**
     * The flat instance has stone up to y 39, so the start block is at 39 and the first block with
     * eight air blocks below is nine jumps up. The spawn is far enough away not to lengthen it.
     */
    static final int ASCENT_JUMPS = 9;

    private static final int PRELOADED_CHUNK_RADIUS = 3;

    private final Env env;
    private final TestTitanNode titan;
    private final JumprunModule module;
    private final LobbyItem item;
    private final RunRecords records;
    private final RunMessages messages;
    private final RecordingLobbyItems lobbyItems;
    private boolean moduleStopped;

    private JumprunFixture(Env env, TestTitanNode titan, JumprunModule module, LobbyItem item, RunRecords records, RunMessages messages, RecordingLobbyItems lobbyItems) {
        this.env = env;
        this.titan = titan;
        this.module = module;
        this.item = item;
        this.records = records;
        this.messages = messages;
        this.lobbyItems = lobbyItems;
    }

    static FinishedRun finished(UUID player, Mode mode, int score) {
        return new FinishedRun(player, "Alex", mode, score, EndReason.FALL, NOW);
    }

    /** The spawn lies west of the usual start spots, so runs head east. */
    static JumprunFixture start(Env env) {
        return start(env, new InMemoryRunRecords());
    }

    static JumprunFixture start(Env env, RunRecords records) {
        return start(env, records, TestBlocks.shippedConfiguration());
    }

    /** As above, with the palettes read from {@code config} instead of the shipped defaults. */
    static JumprunFixture start(Env env, Configuration config) {
        return start(env, new InMemoryRunRecords(), config);
    }

    /** With a leaderboard, whose refreshes go to {@code refreshes} instead of a database writer. */
    static JumprunFixture start(Env env, RunRecords records, Leaderboard leaderboard, Executor refreshes) {
        return start(env, records, TestBlocks.shippedConfiguration(), Optional.of(leaderboard), refreshes);
    }

    /** As above, in a lobby whose height limits are {@code bounds}. */
    static JumprunFixture start(Env env, LobbyHeightBounds bounds) {
        return start(env, new InMemoryRunRecords(), TestBlocks.shippedConfiguration(), Optional.empty(), Runnable::run, bounds);
    }

    private static JumprunFixture start(Env env, RunRecords records, Configuration config) {
        return start(env, records, config, Optional.empty(), Runnable::run);
    }

    private static JumprunFixture start(Env env, RunRecords records, Configuration config, Optional<Leaderboard> leaderboard, Executor refreshes) {
        return start(env, records, config, leaderboard, refreshes, TestBlocks.BOUNDS);
    }

    /** With the module reporting to {@code telemetry}, and a leaderboard if given. */
    static JumprunFixture start(Env env, RunRecords records, Optional<Leaderboard> leaderboard, Executor refreshes, Telemetry telemetry) {
        return start(env, records, TestBlocks.shippedConfiguration(), leaderboard, refreshes, TestBlocks.BOUNDS, telemetry);
    }

    private static JumprunFixture start(Env env, RunRecords records, Configuration config, Optional<Leaderboard> leaderboard, Executor refreshes, LobbyHeightBounds bounds) {
        return start(env, records, config, leaderboard, refreshes, bounds, Telemetry.noop());
    }

    private static JumprunFixture start(Env env, RunRecords records, Configuration config, Optional<Leaderboard> leaderboard, Executor refreshes, LobbyHeightBounds bounds, Telemetry telemetry) {
        TestTitanNode titan = TestTitanNode.attach(env);
        RunMessages messages = new RunMessages();
        RecordingLobbyItems lobbyItems = new RecordingLobbyItems();
        JumprunModule module = new JumprunModule(titan.node(), () -> new Pos(-40.5, GROUND_Y, 0.5), List::of, records, leaderboard, refreshes, env.process().scheduler(), () -> lobbyItems, messages, () -> SEED, new JumprunConfig(config), () -> bounds, CLOCK, telemetry);
        module.start();
        LobbyItem item = new JumprunItems().jumprun(module);
        // What the hotbar column does with the use packet, without depending on it.
        titan.node().addListener(PlayerUseItemEvent.class, event -> item.onUse().handle(event.getPlayer(), event));
        return new JumprunFixture(env, titan, module, item, records, messages, lobbyItems);
    }

    /** A flat instance whose chunks around the origin are loaded, so the course has room. */
    public static Instance loadedInstance(Env env) {
        Instance instance = env.createFlatInstance();
        for (int x = -PRELOADED_CHUNK_RADIUS; x <= PRELOADED_CHUNK_RADIUS; x++) {
            for (int z = -PRELOADED_CHUNK_RADIUS; z <= PRELOADED_CHUNK_RADIUS; z++) {
                instance.loadChunk(x, z).join();
            }
        }
        return instance;
    }

    LobbyItem item() {
        return item;
    }

    JumprunModule module() {
        return module;
    }

    RunRecords records() {
        return records;
    }

    RecordingLobbyItems lobbyItems() {
        return lobbyItems;
    }

    RunMessages messages() {
        return messages;
    }

    /**
     * Ticks long enough for every block that was shown to have landed and every removed one to be
     * gone.
     */
    void settle() {
        for (int tick = 0; tick < AnimatedBlock.ANIMATION_TICKS + 2; tick++) {
            env.tick();
        }
    }

    /** Uses the lobby item the way the hotbar's dispatcher would. */
    void useItem(Player player) {
        item.onUse().handle(player, new PlayerUseItemEvent(player, PlayerHand.MAIN, item.itemStack(), 0));
    }

    /** A move report from the client, as the server turns the movement packet into an event. */
    void move(Player player, Pos to, boolean onGround) {
        env.process().eventHandler().call(new PlayerMoveEvent(player, to, onGround));
    }

    /**
     * A position packet through the player's connection, so Minestom's own listener raises the move
     * event.
     */
    void sendPositionPacket(Player player, Pos to, boolean onGround) {
        // Minestom drops movement until the client has confirmed the last teleport it was sent.
        env.process().packetListener().processClientPacket(new ClientTeleportConfirmPacket(player.getLastSentTeleportId()), player.getPlayerConnection());
        env.process().packetListener().processClientPacket(new ClientPlayerPositionPacket(to, onGround, false), player.getPlayerConnection());
    }

    /**
     * The packet a client sends when only its ground contact changed: Minestom raises no move
     * event for it.
     */
    void sendOnGroundPacket(Player player, boolean onGround) {
        sendClientPacket(player, new ClientPlayerPositionStatusPacket(onGround, false));
    }

    /** Any client packet through the player's connection, so Minestom's own listener answers. */
    void sendClientPacket(Player player, ClientPacket packet) {
        env.process().packetListener().processClientPacket(packet, player.getPlayerConnection());
    }

    /** The player lands on top of a shown block and the client says so. */
    void landOn(Player player, BlockChangePacket block) {
        Point at = block.blockPosition();
        move(player, new Pos(at.blockX() + 0.5, topOf(block), at.blockZ() + 0.5), true);
    }

    /**
     * The client reaches the top of a shown block in the air and only afterwards reports that it
     * stands there, with a status-only packet.
     */
    void settleOn(Player player, BlockChangePacket block) {
        Point at = block.blockPosition();
        sendPositionPacket(player, new Pos(at.blockX() + 0.5, topOf(block), at.blockZ() + 0.5), false);
        sendOnGroundPacket(player, true);
    }

    /** Walkable top of a shown block, read back from the surface its state id belongs to. */
    static double topOf(BlockChangePacket block) {
        Surface surface = surfaceOf(block).orElseThrow(() -> new IllegalArgumentException("not a course surface: " + block));
        return block.blockPosition().blockY() + surface.top();
    }

    /** Whether the packet shows a course block, as opposed to a real block put back. */
    static boolean isCourseBlock(BlockChangePacket block) {
        return surfaceOf(block).isPresent();
    }

    private static Optional<Surface> surfaceOf(BlockChangePacket block) {
        Block shown = Block.fromStateId(block.blockStateId());
        // The look of a material (facing, turn) differs per block, so the type decides.
        return Arrays.stream(Surface.values()).filter(surface -> shown != null && TestBlocks.shipped().of(surface).blocks().stream().anyMatch(material -> material.id() == shown.id())).findFirst();
    }

    /** The block displays of the run's blocks; the runner's glowing outline is not one of them. */
    static List<Entity> blockDisplays(Instance instance) {
        return blockDisplays(instance, false);
    }

    /** The glowing outline displays of the next block. */
    static List<Entity> outlines(Instance instance) {
        return blockDisplays(instance, true);
    }

    private static List<Entity> blockDisplays(Instance instance, boolean glowing) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.BLOCK_DISPLAY).filter(entity -> ((BlockDisplayMeta) entity.getEntityMeta()).isHasGlowingEffect() == glowing).toList();
    }

    /** Stops only the module, to prove nothing runs once it has; {@link #close()} does the rest. */
    void stopModule() {
        if (!moduleStopped) {
            moduleStopped = true;
            module.stop();
        }
    }

    @Override
    public void close() {
        stopModule();
        titan.close();
    }
}
