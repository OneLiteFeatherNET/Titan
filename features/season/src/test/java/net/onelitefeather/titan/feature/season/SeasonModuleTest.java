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
package net.onelitefeather.titan.feature.season;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

@ExtendWith(MicrotusExtension.class)
class SeasonModuleTest {

    private static final Instant BEFORE_WINTER = Instant.parse("2026-11-15T12:00:00Z");
    private static final Instant IN_WINTER = Instant.parse("2026-12-10T12:00:00Z");

    private final Logger moduleLogger = (Logger) LoggerFactory.getLogger(SeasonModule.class);
    private final ListAppender<ILoggingEvent> moduleLines = new ListAppender<>();
    private final Logger readerLogger = (Logger) LoggerFactory.getLogger(SeasonConfigReader.class);
    private final ListAppender<ILoggingEvent> readerLines = new ListAppender<>();

    @TempDir
    Path worlds;

    private String originalZone;
    private Env env;
    private TestTitanNode titan;
    private AdjustableClock clock;
    private CountingServerStop serverStop;
    private int fakeOnline;
    private SeasonModule module;

    /** Counts calls instead of stopping anything. */
    private static final class CountingServerStop implements ServerStop {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public void stop() {
            this.calls.incrementAndGet();
        }

        int calls() {
            return this.calls.get();
        }
    }

    @BeforeEach
    void setUp(Env env) throws IOException {
        this.originalZone = Config.get(SeasonSettings.ZONE_KEY);
        this.env = env;
        this.titan = TestTitanNode.attach(env);
        this.clock = new AdjustableClock(BEFORE_WINTER, ZoneOffset.UTC);
        this.serverStop = new CountingServerStop();
        this.fakeOnline = 0;
        this.moduleLines.start();
        this.moduleLogger.addAppender(this.moduleLines);
        this.readerLines.start();
        this.readerLogger.addAppender(this.readerLines);
        createWorld("winter");
        Config.setProperty("seasons.winter.world", "winter");
        Config.setProperty("seasons.winter.from", "2026-12-01T00:00:00");
        Config.setProperty("seasons.winter.to", "2027-01-07T00:00:00");
    }

    @AfterEach
    void tearDown() {
        if (this.module != null) {
            this.module.stop();
        }
        this.titan.close();
        this.moduleLogger.detachAppender(this.moduleLines);
        this.readerLogger.detachAppender(this.readerLines);
        Config.asProperties().stringPropertyNames().stream().filter(key -> key.startsWith(SeasonSettings.PREFIX)).forEach(Config::clearProperty);
        Config.setProperty(SeasonSettings.ZONE_KEY, this.originalZone);
    }

    private void createWorld(String name) throws IOException {
        Files.writeString(Files.createDirectories(this.worlds.resolve(name)).resolve(SeasonSettings.MAP_FILE_NAME), "{}");
    }

    private void startModule(Optional<String> startedWorld, OnlinePlayers players) {
        SeasonSchedule schedule = new SeasonSchedule(this.clock, this.worlds);
        this.module = new SeasonModule(this.titan.node(), this.env.process().scheduler(), this.clock, schedule, () -> startedWorld, players, this.serverStop);
        this.module.start();
    }

    /** Starts in the default world with a settable, fake player count. */
    private void startInDefaultWorld() {
        startModule(Optional.empty(), () -> this.fakeOnline);
    }

    /**
     * The order {@code PlayerConnection#disconnect()} uses in production: the connection manager
     * forgets the player first, and the player entity is removed, firing the event, next tick.
     * Env connections only flag themselves offline, so the steps are spelled out here.
     */
    private void disconnect(Player player) {
        this.env.process().connection().removePlayer(player.getPlayerConnection());
        player.scheduleNextTick(Entity::remove);
    }

    private void tickMinutes(int minutes) {
        for (int i = 0; i < minutes * SeasonModule.CHECK_INTERVAL_TICKS; i++) {
            this.env.tick();
        }
    }

    private List<String> infoMessages() {
        return this.moduleLines.list.stream().filter(line -> line.getLevel() == Level.INFO).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private List<String> readerWarnings() {
        return this.readerLines.list.stream().filter(line -> line.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    @DisplayName("A window that has begun makes a restart pending after a minute, logged once")
    @Test
    void windowBeginMakesARestartPendingAndLogsOnce() {
        this.fakeOnline = 1;
        startInDefaultWorld();

        this.clock.set(IN_WINTER);
        tickMinutes(1);

        Assertions.assertEquals(1, infoMessages().size(), "got: " + infoMessages());
        Assertions.assertEquals("Restart for season winter pending since " + IN_WINTER, infoMessages().getFirst());
    }

    @DisplayName("A pending restart is not logged again on later checks")
    @Test
    void pendingRestartIsNotLoggedAgain() {
        this.fakeOnline = 1;
        startInDefaultWorld();
        this.clock.set(IN_WINTER);

        tickMinutes(4);

        Assertions.assertEquals(1, infoMessages().size(), "got: " + infoMessages());
    }

    @DisplayName("A season ending makes a restart into the default world pending")
    @Test
    void windowEndMakesARestartIntoTheDefaultWorldPending() {
        this.clock.set(IN_WINTER);
        this.fakeOnline = 1;
        startModule(Optional.of("winter"), () -> this.fakeOnline);

        this.clock.set(Instant.parse("2027-01-08T12:00:00Z"));
        tickMinutes(1);

        Assertions.assertEquals(List.of("Restart to the default world pending since 2027-01-08T12:00:00Z"), infoMessages());
    }

    @DisplayName("Switching the season off again clears the pending restart and never stops")
    @Test
    void resettingTheSwitchClearsThePendingRestart() {
        this.clock.set(IN_WINTER);
        this.fakeOnline = 1;
        startInDefaultWorld();
        tickMinutes(1);

        Config.setProperty("seasons.winter.enabled", "false");
        tickMinutes(1);
        this.fakeOnline = 0;
        tickMinutes(1);

        Assertions.assertTrue(infoMessages().contains("Restart no longer needed"), "got: " + infoMessages());
        Assertions.assertEquals(0, this.serverStop.calls(), "the lobby must not stop after the restart was withdrawn");
    }

    @DisplayName("An empty lobby stops at the minute check when a restart is due")
    @Test
    void emptyLobbyStopsAtTheMinuteCheck() {
        startInDefaultWorld();
        this.clock.set(IN_WINTER);

        tickMinutes(1);

        Assertions.assertEquals(1, this.serverStop.calls());
        Assertions.assertTrue(infoMessages().contains("Stopping lobby for season change"), "got: " + infoMessages());
    }

    @DisplayName("The lobby does not stop while a player is online, however many checks pass")
    @Test
    void occupiedLobbyNeverStops() {
        this.fakeOnline = 1;
        startInDefaultWorld();
        this.clock.set(IN_WINTER);

        tickMinutes(5);

        Assertions.assertEquals(0, this.serverStop.calls());
    }

    @DisplayName("At most one stop is requested however many checks find the lobby empty")
    @Test
    void stopIsRequestedAtMostOnce() {
        startInDefaultWorld();
        this.clock.set(IN_WINTER);

        tickMinutes(3);

        Assertions.assertEquals(1, this.serverStop.calls());
    }

    @DisplayName("An empty lobby with nothing pending does not stop")
    @Test
    void emptyLobbyWithNothingPendingDoesNotStop() {
        startInDefaultWorld();

        tickMinutes(3);

        Assertions.assertEquals(0, this.serverStop.calls());
        Assertions.assertEquals(List.of(), infoMessages());
    }

    @DisplayName("Running in the desired world after a restart schedules nothing")
    @Test
    void desiredWorldAfterRestartSchedulesNothing() {
        this.clock.set(IN_WINTER);
        startModule(Optional.of("winter"), () -> 0);

        tickMinutes(3);

        Assertions.assertEquals(0, this.serverStop.calls());
        Assertions.assertEquals(List.of(), infoMessages());
    }

    @DisplayName("The last player leaving stops the lobby on the next tick, without waiting for the minute check")
    @Test
    void lastPlayerLeavingStopsOnTheNextTick() {
        Instance lobby = this.env.createFlatInstance();
        Player player = this.env.createPlayer(lobby);
        startModule(Optional.empty(), new ConnectionOnlinePlayers(this.env.process()::connection));
        this.clock.set(IN_WINTER);
        tickMinutes(1);
        Assertions.assertEquals(0, this.serverStop.calls(), "the player is still online");

        disconnect(player);
        this.env.tick();
        this.env.tick();

        Assertions.assertEquals(1, this.serverStop.calls());
    }

    @DisplayName("One of two players leaving does not stop the lobby")
    @Test
    void oneOfTwoPlayersLeavingDoesNotStop() {
        Instance lobby = this.env.createFlatInstance();
        Player leaving = this.env.createPlayer(lobby);
        this.env.createPlayer(lobby);
        startModule(Optional.empty(), new ConnectionOnlinePlayers(this.env.process()::connection));
        this.clock.set(IN_WINTER);
        tickMinutes(1);

        disconnect(leaving);
        this.env.tick();
        this.env.tick();

        Assertions.assertEquals(0, this.serverStop.calls());
    }

    @DisplayName("At the disconnect event the leaving player is already gone from the online count")
    @Test
    void leavingPlayerNoLongerCountsAtTheDisconnectEvent() {
        Instance lobby = this.env.createFlatInstance();
        Player player = this.env.createPlayer(lobby);
        ConnectionOnlinePlayers real = new ConnectionOnlinePlayers(this.env.process()::connection);
        AtomicInteger countAtEvent = new AtomicInteger(-1);
        this.env.process().eventHandler().addListener(net.minestom.server.event.player.PlayerDisconnectEvent.class, event -> countAtEvent.set(real.count()));
        Assertions.assertEquals(1, real.count(), "precondition: the player counts while online");

        disconnect(player);
        for (int i = 0; i < 5 && countAtEvent.get() < 0; i++) {
            this.env.tick();
        }

        Assertions.assertEquals(0, countAtEvent.get(), "Minestom removes the player from the connection manager before it fires the event");
        Assertions.assertEquals(0, real.count(), "and it stays gone on the next tick");
    }

    @DisplayName("A season enabled live without a world warns naming the key and schedules nothing")
    @Test
    void liveEnabledSeasonWithoutWorldWarnsAndSchedulesNothing() {
        Config.setProperty("seasons.spring.world", "spring");
        Config.setProperty("seasons.spring.from", "2026-12-01T00:00:00");
        Config.setProperty("seasons.spring.to", "2027-01-07T00:00:00");
        Config.setProperty("seasons.spring.enabled", "false");
        startInDefaultWorld();
        this.clock.set(IN_WINTER);

        Config.setProperty("seasons.spring.enabled", "true");
        tickMinutes(2);

        Assertions.assertTrue(readerWarnings().stream().anyMatch(message -> message.contains("seasons.spring.world")), "got: " + readerWarnings());
        Assertions.assertEquals(0, this.serverStop.calls(), "an invalid configuration must never stop the lobby");
        Assertions.assertEquals(List.of(), infoMessages());
    }

    @DisplayName("A value that turns invalid live warns and neither schedules nor stops, even for the running season")
    @Test
    void liveInvalidValueSchedulesNothing() {
        this.clock.set(IN_WINTER);
        startModule(Optional.of("winter"), () -> 0);

        Config.setProperty("seasons.winter.from", "morgen");
        tickMinutes(2);

        Assertions.assertTrue(readerWarnings().stream().anyMatch(message -> message.contains("seasons.winter.from")), "got: " + readerWarnings());
        Assertions.assertEquals(0, this.serverStop.calls());
        Assertions.assertEquals(List.of(), infoMessages());
    }

    @DisplayName("Once stopped, the module checks nothing any more")
    @Test
    void stoppedModuleChecksNothing() {
        startInDefaultWorld();
        this.module.stop();
        this.clock.set(IN_WINTER);

        tickMinutes(2);

        Assertions.assertEquals(0, this.serverStop.calls());
        this.module = null;
    }
}
