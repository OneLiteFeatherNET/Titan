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
package net.onelitefeather.titan.feature.daytime;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Config;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

@ExtendWith(MicrotusExtension.class)
class DaytimeModuleTest {

    /** 09:00 in Europe/Berlin (CET), so tick 3000. */
    private static final Instant NINE_IN_BERLIN = Instant.parse("2026-01-15T08:00:00Z");
    private static final long BERLIN_NINE_TICKS = 3000L;
    private static final int RUN_INTERVAL_TICKS = 20;

    private final Logger moduleLogger = (Logger) LoggerFactory.getLogger(DaytimeModule.class);
    private final ListAppender<ILoggingEvent> logLines = new ListAppender<>();

    private String originalEnabled;
    private String originalZone;
    private Env env;
    private Instance lobby;
    private AdjustableClock clock;
    private DaytimeModule module;

    @BeforeEach
    void setUp(Env env) {
        this.originalEnabled = Config.get(DaytimeSettings.ENABLED_KEY);
        this.originalZone = Config.get(DaytimeSettings.ZONE_KEY);
        this.env = env;
        this.lobby = env.createFlatInstance();
        this.clock = new AdjustableClock(NINE_IN_BERLIN, ZoneOffset.UTC);
        this.logLines.start();
        this.moduleLogger.addAppender(this.logLines);
    }

    @AfterEach
    void tearDown() {
        if (this.module != null) {
            this.module.stop();
        }
        this.moduleLogger.detachAppender(this.logLines);
        Config.setProperty(DaytimeSettings.ENABLED_KEY, this.originalEnabled);
        Config.setProperty(DaytimeSettings.ZONE_KEY, this.originalZone);
    }

    private void startModule() {
        this.module = new DaytimeModule(this.lobby, this.env.process().scheduler(), this.clock);
        this.module.start();
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.env.tick();
        }
    }

    private List<ILoggingEvent> warnings() {
        return this.logLines.list.stream().filter(line -> line.getLevel() == Level.WARN).toList();
    }

    @DisplayName("Starting freezes the Minestom clock")
    @Test
    void startFreezesTheMinestomClock() {
        this.lobby.defaultClock().rate(1f);

        startModule();

        Assertions.assertEquals(0f, this.lobby.defaultClock().rate(), "only the wall clock may move the lobby time");
    }

    @DisplayName("The time is set right at start, overwriting an earlier midday freeze")
    @Test
    void timeIsSetImmediatelyAtStart() {
        this.lobby.setTime(6000L);

        startModule();

        Assertions.assertEquals(BERLIN_NINE_TICKS, this.lobby.getTime(), "09:00 in Berlin, without waiting for a tick");
    }

    @DisplayName("The time follows the platform clock after the next update")
    @Test
    void timeFollowsTheReplacedClock() {
        startModule();

        this.clock.advance(Duration.ofHours(9));
        tick(RUN_INTERVAL_TICKS);

        Assertions.assertEquals(12000L, this.lobby.getTime(), "18:00 in Berlin");
    }

    @DisplayName("The time does not move between two updates")
    @Test
    void timeStaysConstantBetweenUpdates() {
        startModule();

        this.clock.advance(Duration.ofHours(1));
        tick(RUN_INTERVAL_TICKS - 1);

        Assertions.assertEquals(BERLIN_NINE_TICKS, this.lobby.getTime(), "no update is due before 20 ticks have passed");
    }

    @DisplayName("Over 100 ticks the clock is read at most five times after the immediate first run")
    @Test
    void updatesAtMostOncePerSecond() {
        startModule();
        int readsAfterStart = this.clock.reads();

        tick(100);

        int scheduledRuns = this.clock.reads() - readsAfterStart;
        Assertions.assertTrue(scheduledRuns <= 5, "at most 5 updates in 100 ticks, but there were " + scheduledRuns);
        Assertions.assertTrue(scheduledRuns > 0, "the scheduled task must run at all");
    }

    @DisplayName("Disabled: the lobby goes to midday within the next update")
    @Test
    void disablingSetsMidday() {
        startModule();

        Config.setProperty(DaytimeSettings.ENABLED_KEY, "false");
        tick(RUN_INTERVAL_TICKS);

        Assertions.assertEquals(6000L, this.lobby.getTime(), "midday while daytime.enabled is false");
    }

    @DisplayName("Enabled again: the lobby follows the wall clock without a restart")
    @Test
    void reEnablingFollowsTheWallClockAgain() {
        startModule();
        Config.setProperty(DaytimeSettings.ENABLED_KEY, "false");
        tick(RUN_INTERVAL_TICKS);

        Config.setProperty(DaytimeSettings.ENABLED_KEY, "true");
        tick(RUN_INTERVAL_TICKS);

        Assertions.assertEquals(BERLIN_NINE_TICKS, this.lobby.getTime(), "09:00 in Berlin again");
    }

    @DisplayName("Changing daytime.zone takes effect with the next update")
    @Test
    void zoneChangeAppliesLive() {
        startModule();

        Config.setProperty(DaytimeSettings.ZONE_KEY, "America/New_York");
        tick(RUN_INTERVAL_TICKS);

        Assertions.assertEquals(21000L, this.lobby.getTime(), "03:00 in New York while it is 09:00 in Berlin");
    }

    @DisplayName("An invalid zone at start aborts the startup and names daytime.zone")
    @Test
    void invalidZoneAtStartAbortsStartup() {
        Config.setProperty(DaytimeSettings.ZONE_KEY, "Mars/Olympus");

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> new DaytimeModule(this.lobby, this.env.process().scheduler(), this.clock).start());

        Assertions.assertTrue(thrown.getMessage().contains(DaytimeSettings.ZONE_KEY), "the message must name " + DaytimeSettings.ZONE_KEY + ", was: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getCause().getMessage().contains("Mars/Olympus"), "the reason must name the offending zone, was: " + thrown.getCause().getMessage());
    }

    @DisplayName("An invalid zone at runtime keeps the last valid zone")
    @Test
    void invalidZoneAtRuntimeKeepsTheLastValidZone() {
        startModule();

        Config.setProperty(DaytimeSettings.ZONE_KEY, "Mars/Olympus");
        this.clock.advance(Duration.ofHours(1));
        tick(RUN_INTERVAL_TICKS);

        Assertions.assertEquals(4000L, this.lobby.getTime(), "10:00 in Berlin, the zone that was valid before");
    }

    @DisplayName("An invalid zone at runtime logs a warning naming daytime.zone")
    @Test
    void invalidZoneAtRuntimeLogsAWarning() {
        startModule();

        Config.setProperty(DaytimeSettings.ZONE_KEY, "Mars/Olympus");
        tick(RUN_INTERVAL_TICKS);

        Assertions.assertEquals(1, warnings().size(), "exactly one warning for the changed value");
        String message = warnings().get(0).getFormattedMessage();
        Assertions.assertTrue(message.contains(DaytimeSettings.ZONE_KEY) && message.contains("Mars/Olympus"), "the warning must name the key and the value, was: " + message);
    }

    @DisplayName("A blank zone at runtime keeps the last valid zone and warns once")
    @Test
    void blankZoneAtRuntimeKeepsTheLastValidZone() {
        startModule();

        Config.setProperty(DaytimeSettings.ZONE_KEY, "");
        this.clock.advance(Duration.ofHours(1));
        tick(3 * RUN_INTERVAL_TICKS);

        Assertions.assertEquals(4000L, this.lobby.getTime(), "10:00 in Berlin, the zone that was valid before");
        Assertions.assertEquals(1, warnings().size(), "one warning for the blank value, got: " + warnings());
    }

    @DisplayName("The same invalid zone is warned about only once, a different invalid one again")
    @Test
    void invalidZoneWarningIsLoggedOnlyWhenTheValueChanges() {
        startModule();

        Config.setProperty(DaytimeSettings.ZONE_KEY, "Mars/Olympus");
        tick(3 * RUN_INTERVAL_TICKS);
        Assertions.assertEquals(1, warnings().size(), "three runs with the same invalid value must warn once");

        Config.setProperty(DaytimeSettings.ZONE_KEY, "Mars/Phobos");
        tick(RUN_INTERVAL_TICKS);
        Assertions.assertEquals(2, warnings().size(), "a different invalid value must warn again");
    }

    @DisplayName("A valid zone at runtime produces no warning")
    @Test
    void validRuntimeChangesDoNotWarn() {
        startModule();

        Config.setProperty(DaytimeSettings.ZONE_KEY, "Asia/Tokyo");
        tick(RUN_INTERVAL_TICKS);

        Assertions.assertTrue(warnings().isEmpty(), "no warning expected, got: " + warnings());
    }

    @DisplayName("A lobby without a default clock logs a warning and still follows the wall clock")
    @Test
    void missingDefaultClockLogsAWarning() {
        // Env instances always carry a default clock, so the null-clock case needs a mock.
        Instance clocklessLobby = Mockito.mock(Instance.class);
        this.module = new DaytimeModule(clocklessLobby, this.env.process().scheduler(), this.clock);

        this.module.start();

        Assertions.assertEquals(1, warnings().size(), "one warning for the missing clock");
        Mockito.verify(clocklessLobby).setTime(BERLIN_NINE_TICKS);
    }

    @DisplayName("Restarting at the same wall clock time gives the same lobby time")
    @Test
    void restartDoesNotShiftTheTime() {
        startModule();
        long firstStart = this.lobby.getTime();
        this.module.stop();

        Instance restartedLobby = this.env.createFlatInstance();
        this.module = new DaytimeModule(restartedLobby, this.env.process().scheduler(), new AdjustableClock(NINE_IN_BERLIN, ZoneOffset.UTC));
        this.module.start();

        Assertions.assertEquals(firstStart, restartedLobby.getTime(), "same instant and zone, same ticks");
    }

    @DisplayName("Once stopped, the lobby time is no longer updated")
    @Test
    void stopCancelsTheTask() {
        startModule();
        this.module.stop();

        this.clock.advance(Duration.ofHours(3));
        tick(2 * RUN_INTERVAL_TICKS);

        Assertions.assertEquals(BERLIN_NINE_TICKS, this.lobby.getTime(), "a stopped module must not touch the lobby time");
    }
}
