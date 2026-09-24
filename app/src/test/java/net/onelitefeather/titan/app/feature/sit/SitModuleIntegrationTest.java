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
package net.onelitefeather.titan.app.feature.sit;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage of {@link SitModule} started through {@link ModuleHarness}, exercising the
 * full wiring (config, four listeners, {@link Seats}) the way the lobby actually runs it. Covers
 * the {@code lobby-modules} spec scenario "Sitzen und Aufstehen": click an allowed block, then
 * stand up by sneaking.
 */
@ExtendWith(MicrotusExtension.class)
class SitModuleIntegrationTest {

    private static PlayerBlockInteractEvent clickBlock(Player player, Instance instance, Block block, BlockVec position) {
        return new PlayerBlockInteractEvent(player, PlayerHand.MAIN, instance, block, position, new Vec(0.5, 1, 0.5), BlockFace.TOP);
    }

    private static PlayerPacketEvent sneak(Player player) {
        return new PlayerPacketEvent(player, new ClientInputPacket(false, false, false, false, false, true, false));
    }

    @DisplayName("Clicking an allowed block sits the player down")
    @Test
    void clickingAnAllowedBlockSitsThePlayer(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(new Pos(0, 64, 0));

        try (ModuleHarness harness = ModuleHarness.start(env, new SitModule())) {
            env.process().eventHandler().call(clickBlock(player, instance, Block.fromKey("minecraft:spruce_stairs"), new BlockVec(0, 64, 0)));

            Assertions.assertNotNull(player.getVehicle(), "the player must be sitting after clicking an allowed block");
        }
    }

    @DisplayName("Sneaking while sitting stands the player up at their previous position")
    @Test
    void sneakingWhileSittingStandsUpAtThePreviousPosition(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Pos before = new Pos(2, 65, 2);
        player.teleport(before);

        try (ModuleHarness harness = ModuleHarness.start(env, new SitModule())) {
            env.process().eventHandler().call(clickBlock(player, instance, Block.fromKey("minecraft:spruce_stairs"), new BlockVec(2, 65, 2)));
            Assertions.assertNotNull(player.getVehicle(), "player must be sitting before sneaking");

            env.process().eventHandler().call(sneak(player));

            Assertions.assertNull(player.getVehicle(), "the player must no longer be riding the seat after sneaking");
            Assertions.assertEquals(before.x(), player.getPosition().x(), 0.001);
            Assertions.assertEquals(before.y(), player.getPosition().y(), 0.001);
            Assertions.assertEquals(before.z(), player.getPosition().z(), 0.001);
        }
    }

    @DisplayName("Disconnecting while sitting removes the seat entity")
    @Test
    void disconnectingWhileSittingRemovesTheSeatEntity(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(new Pos(0, 64, 0));

        try (ModuleHarness harness = ModuleHarness.start(env, new SitModule())) {
            env.process().eventHandler().call(clickBlock(player, instance, Block.fromKey("minecraft:spruce_stairs"), new BlockVec(0, 64, 0)));
            var seat = player.getVehicle();
            Assertions.assertNotNull(seat, "player must be sitting before disconnecting");

            env.process().eventHandler().call(new PlayerDisconnectEvent(player));

            Assertions.assertNull(instance.getEntityByUuid(seat.getUuid()), "the seat entity must be removed on disconnect");
        }
    }

    @DisplayName("Clicking a non-allowed block does nothing")
    @Test
    void clickingANonAllowedBlockDoesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(new Pos(0, 64, 0));

        try (ModuleHarness harness = ModuleHarness.start(env, new SitModule())) {
            env.process().eventHandler().call(clickBlock(player, instance, Block.fromKey("minecraft:stone"), new BlockVec(0, 64, 0)));

            Assertions.assertNull(player.getVehicle(), "clicking a non-allowed block must not sit the player down");
        }
    }

    @DisplayName("Once the harness is closed, clicking an allowed block no longer sits the player")
    @Test
    void afterHarnessCloseClickingNoLongerSitsThePlayer(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(new Pos(0, 64, 0));
        ModuleHarness harness = ModuleHarness.start(env, new SitModule());
        harness.close();

        env.process().eventHandler().call(clickBlock(player, instance, Block.fromKey("minecraft:spruce_stairs"), new BlockVec(0, 64, 0)));

        Assertions.assertNull(player.getVehicle(), "no listener may still be attached once the harness is closed");
    }
}
