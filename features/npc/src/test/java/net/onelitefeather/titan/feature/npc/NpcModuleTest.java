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
package net.onelitefeather.titan.feature.npc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.npc.LobbyNpc;
import net.onelitefeather.titan.core.npc.NpcSkin;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.FakePlayerCounts;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Every test gets its own {@code Env}, lobby instance, titan node, module and clock. */
@ExtendWith(MicrotusExtension.class)
class NpcModuleTest {

    private static final String PERMISSION = "titan.navigator.slender";
    private static final Pos STAND = new Pos(76.5, 80, 157.5, -90, 0);

    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-24T20:00:00Z"));
    private final RecordingDeliver deliver = new RecordingDeliver();
    private final FakePermissionService permissions = new FakePermissionService();
    private final FakePlayerCounts counts = new FakePlayerCounts().serveServices("Slender", new ServiceCount("Slender-1", 4, 20));
    private final TestTelemetry testTelemetry = TestTelemetry.create();

    @AfterEach
    void closeTelemetry() {
        this.testTelemetry.close();
    }

    private static LobbyNpc slender(String permission) {
        return new LobbyNpc("slender", "Slender", STAND, new NpcSkin("dGV4dHVyZQ==", "signature"), null, false, permission);
    }

    private NpcModule start(Env env, TestTitanNode titan, Instance lobby, Optional<FakePlayerCounts> readable, LobbyNpc... npcs) {
        NpcModule module = new NpcModule(titan.node(), this.deliver, this.permissions, () -> List.of(npcs), lobby, env.process().scheduler(), Runnable::run, readable.map(counts -> (net.onelitefeather.titan.core.portal.PlayerCounts) counts), this.clock, this.testTelemetry.telemetry());
        module.start();
        return module;
    }

    private static Entity figure(Instance lobby) {
        return lobby.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.MANNEQUIN).findFirst().orElseThrow();
    }

    private static void click(Env env, Player player, Entity figure) {
        env.process().eventHandler().call(new EntityAttackEvent(player, figure));
        env.tick();
        env.tick();
    }

    @DisplayName("The NPC stands at its configured position and wears its skin")
    @Test
    void figureStandsAtConfiguredPositionWithSkin(Env env) {
        Instance lobby = env.createFlatInstance();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(counts), slender(null));
            Entity figure = figure(lobby);
            Assertions.assertTrue(figure.getPosition().samePoint(STAND), "the NPC stands at " + STAND + ", was " + figure.getPosition());
            Assertions.assertNotNull(((MannequinMeta) figure.getEntityMeta()).getProfile(), "the skin travels in the profile");
        }
    }

    @DisplayName("The NPC does not move during the lobby ticks")
    @Test
    void figureDoesNotMove(Env env) {
        Instance lobby = env.createFlatInstance();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(counts), slender(null));
            for (int tick = 0; tick < 100; tick++) {
                env.tick();
            }
            Assertions.assertTrue(figure(lobby).getPosition().samePoint(STAND), "after 100 ticks the NPC still stands where it was placed");
        }
    }

    @DisplayName("A left click sends the player to the most populated ready service of the task")
    @Test
    void clickJoinsTheReadyService(Env env) {
        Instance lobby = env.createFlatInstance();
        Player player = env.createPlayer(lobby);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(counts), slender(null));
            click(env, player, figure(lobby));
            Assertions.assertEquals(List.of(RecordingDeliver.Delivery.toServer(player, "Slender-1")), this.deliver.deliveries(), "the click joins the service the navigator would pick");
        }
    }

    @DisplayName("A second click within the cooldown does not send the player again")
    @Test
    void secondClickWithinCooldownIsIgnored(Env env) {
        Instance lobby = env.createFlatInstance();
        Player player = env.createPlayer(lobby);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(counts), slender(null));
            click(env, player, figure(lobby));
            this.clock.advance(Duration.ofMillis(500));
            click(env, player, figure(lobby));
            Assertions.assertEquals(1, this.deliver.deliveries().size(), "the second click 0.5 s later is inside the cooldown");
        }
    }

    @DisplayName("A player without the NPC's permission is not sent")
    @Test
    void withoutPermissionNothingHappens(Env env) {
        Instance lobby = env.createFlatInstance();
        Player player = env.createPlayer(lobby);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(counts), slender(PERMISSION));
            click(env, player, figure(lobby));
            Assertions.assertTrue(this.deliver.deliveries().isEmpty(), "without " + PERMISSION + " the click sends nobody");
        }
    }

    @DisplayName("A player with the NPC's permission is sent")
    @Test
    void withPermissionTheClickJoins(Env env) {
        Instance lobby = env.createFlatInstance();
        Player player = env.createPlayer(lobby);
        this.permissions.grant(player.getUuid(), PERMISSION);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(counts), slender(PERMISSION));
            click(env, player, figure(lobby));
            Assertions.assertEquals(1, this.deliver.deliveries().size(), "with the permission the click joins");
        }
    }

    @DisplayName("A task whose services are all full sends nobody")
    @Test
    void fullTaskSendsNobody(Env env) {
        Instance lobby = env.createFlatInstance();
        Player player = env.createPlayer(lobby);
        FakePlayerCounts full = new FakePlayerCounts().serveServices("Slender", new ServiceCount("Slender-1", 20, 20));
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(full), slender(null));
            click(env, player, figure(lobby));
            Assertions.assertTrue(this.deliver.deliveries().isEmpty(), "no ready service means nobody is sent");
        }
    }

    @DisplayName("A task that cannot be read sends nobody")
    @Test
    void unreadableTaskSendsNobody(Env env) {
        Instance lobby = env.createFlatInstance();
        Player player = env.createPlayer(lobby);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.empty(), slender(null));
            click(env, player, figure(lobby));
            Assertions.assertTrue(this.deliver.deliveries().isEmpty(), "without a provider nothing is joined");
        }
    }

    @DisplayName("Left and right click are both a click; a click on another entity is not")
    @Test
    void onlyTheNpcIsClickable(Env env) {
        Instance lobby = env.createFlatInstance();
        Player player = env.createPlayer(lobby);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            start(env, titan, lobby, Optional.of(counts), slender(null));
            Entity other = new Entity(EntityType.ZOMBIE);
            other.setInstance(lobby, STAND).join();
            click(env, player, other);
            Assertions.assertTrue(this.deliver.deliveries().isEmpty(), "a zombie next to the NPC does not join");
        }
    }
}
