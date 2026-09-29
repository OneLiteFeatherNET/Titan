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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Config;
import io.avaje.inject.BeanScope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.platform.luckperms.LuckPermsPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/**
 * Builds the real Avaje Inject {@link BeanScope} of the {@code cloudnet} variant and drives the
 * season column through it: the started world comes from the window, and once the window ends the
 * restart is pending and, with nobody online, the (fake) stop is requested. The clock, the
 * {@code worlds/} directory, the scheduler, the player count and the stop are the only things
 * replaced. Lives in the column's package name because its seams are package-private.
 */
@ExtendWith(MicrotusExtension.class)
@Timeout(30)
class SeasonRestartWiringTest {

    private static final Instant IN_WINTER = Instant.parse("2026-12-24T12:00:00Z");
    private static final Instant AFTER_WINTER = Instant.parse("2027-01-07T12:00:00Z");

    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> moduleLines = new ListAppender<>();

    @TempDir
    Path worlds;

    private String originalZone;
    private Env env;
    private TestTitanNode titan;
    private AdjustableClock clock;
    private AtomicInteger stops;
    private int online;
    private BeanScope scope;

    private static final class AdjustableClock extends Clock {

        private Instant instant;

        AdjustableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.instant;
        }
    }

    @BeforeEach
    void setUp(Env env) throws IOException {
        this.originalZone = Config.get("seasons.zone");
        this.env = env;
        this.titan = TestTitanNode.attach(env);
        this.clock = new AdjustableClock(IN_WINTER);
        this.stops = new AtomicInteger();
        this.online = 0;
        this.moduleLines.start();
        this.rootLogger.addAppender(this.moduleLines);
        Files.writeString(Files.createDirectories(this.worlds.resolve("winter-map")).resolve("map.json"), "{}");
        Config.setProperty("seasons.winter.world", "winter-map");
        Config.setProperty("seasons.winter.from", "2026-12-01T00:00:00");
        Config.setProperty("seasons.winter.to", "2027-01-07T00:00:00");
    }

    @AfterEach
    void tearDown() {
        if (this.scope != null) {
            this.scope.close();
        }
        this.titan.close();
        this.rootLogger.detachAppender(this.moduleLines);
        Config.asProperties().stringPropertyNames().stream().filter(key -> key.startsWith("seasons.")).forEach(Config::clearProperty);
        Config.setProperty("seasons.zone", this.originalZone);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void startScope() {
        ServerStop stop = this.stops::incrementAndGet;
        OnlinePlayers players = () -> this.online;
        // Named mock, not the plain mock(Type) overload - see docs/lobby-modules.md, "Permission-Plattform".
        this.scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).bean(Clock.class, this.clock).bean(Scheduler.class, this.env.process().scheduler()).bean(FeatureNode.TITAN_NODE, EventNode.class, (EventNode<Event>) this.titan.node()).bean(SeasonSchedule.class, new SeasonSchedule(this.clock, this.worlds)).bean(ServerStop.class, stop).bean(OnlinePlayers.class, players).build();
    }

    private void tickOneMinute() {
        for (int i = 0; i < SeasonModule.CHECK_INTERVAL_TICKS; i++) {
            this.env.tick();
        }
    }

    private boolean logged(String fragment) {
        return this.moduleLines.list.stream().anyMatch(line -> line.getFormattedMessage().contains(fragment));
    }

    @DisplayName("The lobby starts in the season world while its window is open")
    @Test
    void lobbyStartsInTheSeasonWorld() {
        startScope();

        Assertions.assertTrue(logged("Lobby world winter-map (season winter)"), "the started world must be the season world, got: " + this.moduleLines.list);
    }

    @DisplayName("When the window ends with players online the restart is pending, not stopped")
    @Test
    void windowEndWithPlayersMakesTheRestartPending() {
        this.online = 1;
        startScope();

        this.clock.instant = AFTER_WINTER;
        tickOneMinute();

        Assertions.assertTrue(logged("Restart to the default world pending"), "a restart to the default world must be pending, got: " + this.moduleLines.list);
        Assertions.assertEquals(0, this.stops.get(), "a busy lobby must not be stopped");
    }

    @DisplayName("When the window ends and nobody is online the server is stopped")
    @Test
    void windowEndWithoutPlayersStopsTheServer() {
        startScope();

        this.clock.instant = AFTER_WINTER;
        tickOneMinute();

        Assertions.assertEquals(1, this.stops.get(), "an empty lobby must request exactly one stop");
    }
}
