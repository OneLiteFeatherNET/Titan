/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.tag;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.random.RandomGenerator;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.SetTitleTextPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyPlayerBusyEvent;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Env coverage of {@link TagModule}: the rules that need real players, instances and events. Time
 * is an {@link AdjustableClock}, the random source is seeded, and every test starts and stops its
 * own module, so no test depends on another.
 */
@ExtendWith(MicrotusExtension.class)
class TagModuleIntegrationTest {

    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    /** A player with the connection that carries their packets. */
    record Seat(TestConnection connection, Player player) {
    }

    /** One running module with its fixtures; closing it stops the module and the node. */
    static final class Round implements AutoCloseable {

        final TagModule module;
        final FakePlayerSettings settings = new FakePlayerSettings();
        final AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);
        private final TestTitanNode titanNode;

        private Round(Env env) {
            this.titanNode = TestTitanNode.attach(env);
            EventNode<Event> titan = this.titanNode.node();
            RandomGenerator random = new Random(7L);
            this.module = new TagModule(titan, env.process().scheduler(), this.clock, Telemetry.noop(), this.settings, new SettingsRegistry(), random);
            this.module.start();
        }

        static Round start(Env env) {
            return new Round(env);
        }

        /** Connects a player to {@code instance} without switching the tag setting on. */
        Seat connect(Env env, Instance instance) {
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            return new Seat(connection, player);
        }

        /** Switches the tag setting on for {@code seat}, as the settings menu would. */
        void enable(Seat seat) {
            this.settings.set(seat.player().getUuid(), TagSettings.ENABLED, true);
        }

        @Override
        public void close() {
            this.module.stop();
            this.titanNode.close();
        }
    }

    static Seat enabledSeat(Env env, Round round, Instance instance) {
        Seat seat = round.connect(env, instance);
        round.enable(seat);
        return seat;
    }

    /** The one of the two players who holds the role, judged by the glow the tagger gets. */
    static Seat taggerOf(Seat first, Seat second) {
        boolean firstGlows = first.player().isGlowing();
        boolean secondGlows = second.player().isGlowing();
        Assertions.assertTrue(firstGlows ^ secondGlows, "exactly one of the two players must glow as the tagger");
        return firstGlows ? first : second;
    }

    static Seat otherOf(Seat tagger, Seat first, Seat second) {
        return tagger == first ? second : first;
    }

    /** The only one of {@code seats} who glows; fails if none or several do. */
    static Seat holderOf(Seat... seats) {
        Seat holder = null;
        for (Seat seat : seats) {
            if (seat.player().isGlowing()) {
                Assertions.assertNull(holder, "at most one player may glow as the tagger");
                holder = seat;
            }
        }
        Assertions.assertNotNull(holder, "exactly one player must glow as the tagger");
        return holder;
    }

    @DisplayName("Damage between two players in the game is cancelled, so it brings neither damage nor knockback")
    @Test
    void damageBetweenActivePlayersIsCancelled(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat attacker = enabledSeat(env, round, instance);
            Seat victim = enabledSeat(env, round, instance);
            EntityDamageEvent damage = new EntityDamageEvent(victim.player(), Damage.fromEntity(attacker.player(), 2f), null);

            env.process().eventHandler().call(damage);

            Assertions.assertTrue(damage.isCancelled(), "damage between two players in the game must be cancelled");
        }
    }

    @DisplayName("Damage to a player who is not in the game is left alone")
    @Test
    void damageToAnInactivePlayerIsNotCancelled(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat attacker = enabledSeat(env, round, instance);
            Seat outsider = round.connect(env, instance);
            EntityDamageEvent damage = new EntityDamageEvent(outsider.player(), Damage.fromEntity(attacker.player(), 2f), null);

            env.process().eventHandler().call(damage);

            Assertions.assertFalse(damage.isCancelled(), "a player outside the game keeps the protection they had without tag");
        }
    }

    @DisplayName("The tagger's hit passes the role to the target")
    @Test
    void taggerHitPassesTheRole(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat tagger = taggerOf(first, second);
            Seat target = otherOf(tagger, first, second);

            env.process().eventHandler().call(new EntityAttackEvent(tagger.player(), target.player()));

            Assertions.assertTrue(target.player().isGlowing(), "the target must hold the role after the hit");
            Assertions.assertFalse(tagger.player().isGlowing(), "the former tagger must no longer glow");
        }
    }

    @DisplayName("The player who is hit by a passed role gets a title")
    @Test
    void passedRoleShowsTheTitleToTheTarget(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat tagger = taggerOf(first, second);
            Seat target = otherOf(tagger, first, second);
            Collector<SetTitleTextPacket> titles = target.connection().trackIncoming(SetTitleTextPacket.class);

            env.process().eventHandler().call(new EntityAttackEvent(tagger.player(), target.player()));

            titles.assertSingle();
        }
    }

    @DisplayName("A hit by a player who is not the tagger changes nothing")
    @Test
    void hitWithoutTheRoleChangesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat tagger = taggerOf(first, second);
            Seat other = otherOf(tagger, first, second);

            env.process().eventHandler().call(new EntityAttackEvent(other.player(), tagger.player()));

            Assertions.assertTrue(tagger.player().isGlowing(), "the tagger must keep the role");
            Assertions.assertFalse(other.player().isGlowing(), "the non-tagger must not become the tagger");
        }
    }

    @DisplayName("Leaving the lobby instance ends a player's part in the game")
    @Test
    void leavingTheInstanceEndsThePart(Env env) {
        Instance instance = env.createFlatInstance();
        Instance elsewhere = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat tagger = taggerOf(first, second);

            tagger.player().setInstance(elsewhere).join();

            Assertions.assertFalse(tagger.player().isGlowing(), "a player who left the instance must not keep the glow");
            Assertions.assertFalse(otherOf(tagger, first, second).player().isGlowing(), "one player left alone has no tagger");
        }
    }

    @DisplayName("A disconnecting tagger hands the role to one of the players who stay")
    @Test
    void disconnectingTaggerHandsOverTheRole(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat third = enabledSeat(env, round, instance);
            Seat tagger = holderOf(first, second, third);

            env.process().eventHandler().call(new PlayerDisconnectEvent(tagger.player()));

            long stayingTaggers = java.util.stream.Stream.of(first, second, third).filter(seat -> seat != tagger && seat.player().isGlowing()).count();
            Assertions.assertEquals(1, stayingTaggers, "exactly one of the remaining players must become the tagger");
        }
    }

    @DisplayName("The tagger glows and every active player sees who the tagger is in the action bar")
    @Test
    void taggerGlowsAndIsShownInTheActionBar(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = round.connect(env, instance);
            Seat second = round.connect(env, instance);
            Collector<ActionBarPacket> bars = second.connection().trackIncoming(ActionBarPacket.class);

            round.enable(first);
            round.enable(second);

            Assertions.assertTrue(first.player().isGlowing() || second.player().isGlowing(), "the tagger must glow");
            Assertions.assertFalse(bars.collect().isEmpty(), "every active player must see the action bar with the tagger");
        }
    }

    @DisplayName("Particles appear around the tagger for the players of its instance only")
    @Test
    void particlesStayInTheInstance(Env env) {
        Instance instance = env.createFlatInstance();
        Instance elsewhere = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat outsider = round.connect(env, elsewhere);
            Collector<ParticlePacket> inside = second.connection().trackIncoming(ParticlePacket.class);
            Collector<ParticlePacket> outside = outsider.connection().trackIncoming(ParticlePacket.class);

            for (int tick = 0; tick < 20; tick++) {
                env.tick();
            }

            Assertions.assertFalse(inside.collect().isEmpty(), "players of the tagger's instance must see the particles");
            Assertions.assertTrue(outside.collect().isEmpty(), "players of another instance must see no particles");
        }
    }

    @DisplayName("No scoreboard team is created for the tagger")
    @Test
    void noScoreboardTeamIsCreated(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            int teamsBefore = MinecraftServer.getTeamManager().getTeams().size();

            enabledSeat(env, round, instance);
            enabledSeat(env, round, instance);

            Assertions.assertEquals(teamsBefore, MinecraftServer.getTeamManager().getTeams().size(), "the tag game must not add teams, which would clash with name prefixes of other modules");
        }
    }

    @DisplayName("A hit while the attacker glides with an elytra passes no role")
    @Test
    void hitWhileGlidingPassesNoRole(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat tagger = taggerOf(first, second);
            Seat target = otherOf(tagger, first, second);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(tagger.player()));

            env.process().eventHandler().call(new EntityAttackEvent(tagger.player(), target.player()));

            Assertions.assertTrue(tagger.player().isGlowing(), "the role must stay frozen while the tagger glides");
        }
    }

    @DisplayName("A player on a jump and run leaves the game and the role goes to the others")
    @Test
    void busyPlayerLeavesTheGame(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat tagger = taggerOf(first, second);

            env.process().eventHandler().call(new LobbyPlayerBusyEvent(tagger.player(), true));

            Assertions.assertFalse(tagger.player().isGlowing(), "a busy player must lose the role");
            Assertions.assertFalse(otherOf(tagger, first, second).player().isGlowing(), "one player left alone has no tagger");
        }
    }

    @DisplayName("A player who is no longer busy rejoins the game")
    @Test
    void finishedPlayerRejoinsTheGame(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = Round.start(env)) {
            Seat first = enabledSeat(env, round, instance);
            Seat second = enabledSeat(env, round, instance);
            Seat tagger = taggerOf(first, second);
            env.process().eventHandler().call(new LobbyPlayerBusyEvent(tagger.player(), true));

            env.process().eventHandler().call(new LobbyPlayerBusyEvent(tagger.player(), false));

            Assertions.assertTrue(first.player().isGlowing() ^ second.player().isGlowing(), "after the run the two players must play again with exactly one tagger");
        }
    }
}
