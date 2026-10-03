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
package net.onelitefeather.titan.feature.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import org.mockito.Mockito;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyReturnToSpawnEvent;
import net.onelitefeather.titan.core.module.SpawnReturn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class LobbySpawnReturnTest {

    private static final Pos SPAWN = new Pos(5, 64, 5);
    private static final Pos ROOF = new Pos(40, 90, 40);

    @DisplayName("With a spawn point the event fires once, before the teleport, and the player is returned")
    @Test
    void firesEventOnceBeforeTeleport(Env env) {
        Player player = playerOnRoof(env);
        List<Pos> positionsAtEvent = new ArrayList<>();
        EventNode<Event> node = recordPositionsAtEvent(env, positionsAtEvent);
        try {
            LobbySpawnReturn spawnReturn = new LobbySpawnReturn(() -> SPAWN, new SpawnMessages());

            SpawnReturn.Result result = spawnReturn.sendToSpawn(player);

            Assertions.assertEquals(SpawnReturn.Result.RETURNED, result, "a map with a spawn point must return the player");
            Assertions.assertEquals(List.of(ROOF), positionsAtEvent, "the event must fire exactly once while the player is still on the roof");
            Assertions.assertEquals(SPAWN, player.getPosition(), "the player must end up at the spawn point");
        } finally {
            env.process().eventHandler().removeChild(node);
        }
    }

    @DisplayName("Without a spawn point there is no event, no teleport and NO_SPAWN")
    @Test
    void noSpawnPointDoesNothing(Env env) {
        Player player = playerOnRoof(env);
        List<Pos> positionsAtEvent = new ArrayList<>();
        EventNode<Event> node = recordPositionsAtEvent(env, positionsAtEvent);
        try {
            LobbySpawnReturn spawnReturn = new LobbySpawnReturn(() -> null, new SpawnMessages());

            SpawnReturn.Result result = spawnReturn.sendToSpawn(player);

            Assertions.assertEquals(SpawnReturn.Result.NO_SPAWN, result, "a map without a spawn point must report NO_SPAWN");
            Assertions.assertTrue(positionsAtEvent.isEmpty(), "no event may fire without a spawn point");
            Assertions.assertEquals(ROOF, player.getPosition(), "the player must not be moved");
        } finally {
            env.process().eventHandler().removeChild(node);
        }
    }

    @DisplayName("A failed teleport is logged once as a warning with the player and the cause")
    @Test
    void failedTeleportIsLoggedOnce() {
        UUID uuid = UUID.randomUUID();
        IllegalStateException failure = new IllegalStateException("chunk could not load");
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUuid()).thenReturn(uuid);
        Mockito.when(player.teleport(SPAWN, Vec.ZERO)).thenReturn(CompletableFuture.failedFuture(failure));
        try (CapturedLog log = new CapturedLog(LobbySpawnReturn.class)) {
            SpawnReturn.Result result = new LobbySpawnReturn(() -> SPAWN, new SpawnMessages()).sendToSpawn(player);

            Assertions.assertEquals(SpawnReturn.Result.RETURNED, result, "the teleport was requested, so the result stays RETURNED");
            Assertions.assertEquals(1, log.warnings().size(), "exactly one warning must be logged");
            var line = log.warnings().getFirst();
            Assertions.assertEquals("Could not teleport player to spawn", line.getFormattedMessage(), "the warning text");
            Assertions.assertEquals(uuid, CapturedLog.valueOf(line, "player"), "the warning must name the player");
            Assertions.assertSame(failure, CapturedLog.causeOf(line), "the failure must be the logged cause");
        }
    }

    @DisplayName("A gliding player loses the glide momentum and stays at the spawn point")
    @Test
    void glidingPlayerLosesMomentum(Env env) {
        Player player = playerOnRoof(env);
        player.setEquipment(EquipmentSlot.CHESTPLATE, ItemStack.of(Material.ELYTRA));
        player.setFlyingWithElytra(true);
        player.setVelocity(new Vec(400, -200, 400));

        new LobbySpawnReturn(() -> SPAWN, new SpawnMessages()).sendToSpawn(player);
        env.tick();
        env.tick();
        env.tick();

        // Gravity still pulls the player down from the spawn height; only the glide must be gone.
        Assertions.assertEquals(0, player.getVelocity().x(), "the horizontal glide momentum must be gone");
        Assertions.assertEquals(0, player.getVelocity().z(), "the horizontal glide momentum must be gone");
        Assertions.assertEquals(SPAWN.x(), player.getPosition().x(), 0.01, "the player must not drift away from the spawn point");
        Assertions.assertEquals(SPAWN.z(), player.getPosition().z(), 0.01, "the player must not drift away from the spawn point");
    }

    private static Player playerOnRoof(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(ROOF);
        return player;
    }

    private static EventNode<Event> recordPositionsAtEvent(Env env, List<Pos> sink) {
        EventNode<Event> node = EventNode.all("spawn-return-test");
        node.addListener(LobbyReturnToSpawnEvent.class, event -> sink.add(event.getPlayer().getPosition()));
        env.process().eventHandler().addChild(node);
        return node;
    }
}
