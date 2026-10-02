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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** A run takes the elytra off, so the space bar cannot end it, and gives the loadout back. */
@ExtendWith(MicrotusExtension.class)
class JumprunLoadoutTest {

    private static StartedRun startWearingTheLoadout(Env env, JumprunFixture fixture) {
        return StartedRun.startAfter(env, fixture, fixture.lobbyItems()::equip);
    }

    private static void assertLoadoutBack(JumprunFixture fixture, Player player) {
        assertEquals(RecordingLobbyItems.ELYTRA, player.getEquipment(EquipmentSlot.CHESTPLATE), "the elytra is back");
        assertEquals(RecordingLobbyItems.SLOT_ZERO, player.getInventory().getItemStack(0), "the hotbar is back");
        assertEquals(2, fixture.lobbyItems().equipped().size(), "once before the run, once after it");
    }

    @Test
    void startingARunEmptiesTheChestSlot(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = startWearingTheLoadout(env, fixture);

            assertTrue(fixture.module().isRunning(run.player()));
            assertEquals(ItemStack.AIR, run.player().getEquipment(EquipmentSlot.CHESTPLATE), "no elytra, so no gliding");
        }
    }

    @Test
    void aStartThatFindsNoRoomLeavesTheElytraOn(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            Player player = env.createConnection().connect(JumprunFixture.loadedInstance(env), StartedRun.STAND);
            fixture.lobbyItems().equip(player);
            player.refreshOnGround(false);

            fixture.useItem(player);

            assertFalse(fixture.module().isRunning(player));
            assertEquals(RecordingLobbyItems.ELYTRA, player.getEquipment(EquipmentSlot.CHESTPLATE));
        }
    }

    @Test
    void anElytraGlideDuringARunDoesNotEndIt(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = startWearingTheLoadout(env, fixture);

            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(run.player()));

            assertTrue(fixture.module().isRunning(run.player()), "gliding is not an end reason any more");
        }
    }

    @Test
    void abortingGivesTheLoadoutBack(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = startWearingTheLoadout(env, fixture);

            fixture.useItem(run.player());

            assertLoadoutBack(fixture, run.player());
        }
    }

    @Test
    void aFallGivesTheLoadoutBack(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = startWearingTheLoadout(env, fixture);

            fixture.move(run.player(), StartedRun.STAND.withY(JumprunFixture.GROUND_Y - 3.5), false);

            assertLoadoutBack(fixture, run.player());
        }
    }

    @Test
    void dyingGivesTheLoadoutBack(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = startWearingTheLoadout(env, fixture);

            env.process().eventHandler().call(new PlayerDeathEvent(run.player(), Component.empty(), Component.empty()));

            assertLoadoutBack(fixture, run.player());
        }
    }

    @Test
    void leavingTheInstanceGivesTheLoadoutBack(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = startWearingTheLoadout(env, fixture);

            run.player().setInstance(env.createFlatInstance(), StartedRun.STAND.add(6, 0, 0)).join();

            assertLoadoutBack(fixture, run.player());
        }
    }

    @Test
    void disconnectingDoesNotEquipTheLeavingPlayer(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = startWearingTheLoadout(env, fixture);

            env.process().eventHandler().call(new PlayerDisconnectEvent(run.player()));

            assertEquals(1, fixture.lobbyItems().equipped().size(), "only the equip before the run");
        }
    }

    @Test
    void shuttingDownDoesNotEquipAnyone(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            startWearingTheLoadout(env, fixture);

            fixture.stopModule();

            assertEquals(1, fixture.lobbyItems().equipped().size(), "only the equip before the run");
        }
    }
}
