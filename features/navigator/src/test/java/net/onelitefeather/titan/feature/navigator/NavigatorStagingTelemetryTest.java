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
package net.onelitefeather.titan.feature.navigator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakeOnDemandServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * The instruments of the staging services, read back from an in-memory reader: name, unit, value
 * and attributes for each outcome, and the INFO lines of the start and the stop. Unit level, with
 * fakes and an explicit clock; no tick and no sleep.
 */
class NavigatorStagingTelemetryTest {

    private static final Destination STAGING = Destination.SLENDER_STAGING;
    private static final String TASK = STAGING.task();
    private static final String SERVICE = "Slender-Staging-1";
    private static final String PLAYER = "0f8fad5b-d9cb-469f-a165-8c4e9b1e2f6a";
    private static final Duration LIMIT = Duration.ofSeconds(600);

    private static final String STARTS = "titan.navigator.staging.starts";
    private static final String STOPS = "titan.navigator.staging.stops";
    private static final String WAIT = "titan.navigator.staging.wait";
    private static final AttributeKey<String> DESTINATION = AttributeKey.stringKey("destination");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");

    private final InMemoryMetricReader reader = InMemoryMetricReader.create();
    private final SdkMeterProvider meters = SdkMeterProvider.builder().registerMetricReader(this.reader).build();
    private final FakeOnDemandServices services = new FakeOnDemandServices();
    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-26T12:00:00Z"));
    private final Ports ports = new Ports();
    private final NavigatorTelemetry telemetry = new NavigatorTelemetry(new Telemetry(TracerProvider.noop().get("test"), this.meters.get("test")));
    private final StagingStarts<String> starts = new StagingStarts<>(this.services, this.clock, this.ports, this.ports, this.telemetry);
    private final StagingIdle idle = new StagingIdle(this.services, this.clock, LIMIT, this.telemetry);

    @AfterEach
    void closeMeters() {
        this.meters.close();
    }

    @DisplayName("The starts counter is named titan.navigator.staging.starts with the unit {start}")
    @Test
    void startsCounterHasItsNameAndUnit() {
        click();

        MetricData metric = metric(STARTS);
        Assertions.assertEquals("{start}", metric.getUnit(), "one start is counted in the unit {start}");
    }

    @DisplayName("The stops counter is named titan.navigator.staging.stops with the unit {stop}")
    @Test
    void stopsCounterHasItsNameAndUnit() {
        stopAfterTheLimit(this.idle, this.services);

        MetricData metric = metric(STOPS);
        Assertions.assertEquals("{stop}", metric.getUnit(), "one stop is counted in the unit {stop}");
    }

    @DisplayName("The wait histogram is named titan.navigator.staging.wait with the unit s")
    @Test
    void waitHistogramHasItsNameAndUnit() {
        click();
        this.clock.advance(Duration.ofSeconds(12));
        this.services.serve(TASK, new ServiceState(SERVICE, Phase.RUNNING, 0, 10));
        refreshWaiting();

        MetricData metric = metric(WAIT);
        Assertions.assertEquals("s", metric.getUnit(), "the wait is measured in seconds");
    }

    @DisplayName("A click that starts a service counts one requested start for its destination")
    @Test
    void requestedStartIsCounted() {
        click();

        Assertions.assertEquals(1, counter(STARTS, attributes("requested")), "the click that started the service is counted as requested");
        Assertions.assertEquals(0, counter(STARTS, attributes("failed")), "a start that was accepted is not counted as failed");
    }

    @DisplayName("A click while the service is preparing counts one skipped start, and starts nothing")
    @Test
    void preparingServiceCountsSkippedStart() {
        this.services.serve(TASK, new ServiceState(SERVICE, Phase.STARTING, 0, 0));

        click();

        Assertions.assertEquals(1, counter(STARTS, attributes("skipped_starting")), "a click on a preparing service is counted as skipped");
        Assertions.assertEquals(0, this.services.starts(), "a preparing service must not be started again");
    }

    @DisplayName("A click within the cooldown after a start counts one cooldown and starts nothing")
    @Test
    void cooldownIsCounted() {
        click();
        this.services.serve(TASK, new ServiceState(SERVICE, Phase.STOPPED, 0, 10));

        click();

        Assertions.assertEquals(1, counter(STARTS, attributes("cooldown")), "the second click within 30 seconds is counted as cooldown");
        Assertions.assertEquals(1, this.services.starts(), "the cooldown must block a second start");
    }

    @DisplayName("A start the provider refuses counts one failed start")
    @Test
    void refusedStartIsCountedAsFailed() {
        this.services.rejectStarts(true);

        click();

        Assertions.assertEquals(1, counter(STARTS, attributes("failed")), "a refused start is counted as failed");
        Assertions.assertEquals(0, counter(STARTS, attributes("requested")), "a refused start is not counted as requested");
    }

    @DisplayName("An idle stop that the provider accepts counts one requested stop")
    @Test
    void acceptedStopIsCountedAsRequested() {
        stopAfterTheLimit(this.idle, this.services);

        Assertions.assertEquals(1, counter(STOPS, attributes("requested")), "the accepted stop is counted as requested");
        Assertions.assertEquals(List.of(SERVICE), this.services.stopped(), "the service was stopped");
    }

    @DisplayName("An idle stop that the provider refuses counts one failed stop")
    @Test
    void refusedStopIsCountedAsFailed() {
        StagingIdle refusing = new StagingIdle(new RefusingStops(this.services), this.clock, LIMIT, this.telemetry);

        stopAfterTheLimit(refusing, this.services);

        Assertions.assertEquals(1, counter(STOPS, attributes("failed")), "the refused stop is counted as failed");
        Assertions.assertEquals(0, counter(STOPS, attributes("requested")), "a refused stop is not counted as requested");
    }

    @DisplayName("A waiting player who is connected once the service is ready records the seconds waited as ready")
    @Test
    void readyWaitRecordsTheSecondsWaited() {
        click();
        this.clock.advance(Duration.ofSeconds(12));
        this.services.serve(TASK, new ServiceState(SERVICE, Phase.RUNNING, 0, 10));

        refreshWaiting();

        HistogramPointData wait = wait("ready");
        Assertions.assertEquals(1L, wait.getCount(), "one player was connected when the service was ready");
        Assertions.assertEquals(12.0, wait.getSum(), 0.0001, "the player waited 12 seconds");
    }

    @DisplayName("A waiting player refused at the limit records the seconds waited as timeout")
    @Test
    void timeoutWaitRecordsTheLimit() {
        click();
        this.clock.advance(Duration.ofSeconds(90));

        refreshWaiting();

        HistogramPointData wait = wait("timeout");
        Assertions.assertEquals(1L, wait.getCount(), "one player was refused at the limit");
        Assertions.assertEquals(90.0, wait.getSum(), 0.0001, "the player waited the full 90 seconds");
    }

    @DisplayName("A waiting player who logs out records the seconds waited as left")
    @Test
    void leavingWaitRecordsTheSecondsWaited() {
        click();
        this.clock.advance(Duration.ofSeconds(5));

        this.starts.disconnected(PLAYER);

        HistogramPointData wait = wait("left");
        Assertions.assertEquals(1L, wait.getCount(), "one waiting player left");
        Assertions.assertEquals(5.0, wait.getSum(), 0.0001, "the player waited 5 seconds before leaving");
    }

    @DisplayName("The start logs one INFO line that names the task, and no player")
    @Test
    void startLogsAnInfoLineWithTheTask() {
        try (InfoLines log = new InfoLines(StagingStarts.class)) {
            click();

            List<ILoggingEvent> infos = log.infos();
            Assertions.assertEquals(1, infos.size(), "one accepted start logs one INFO line");
            Assertions.assertTrue(infos.getFirst().getFormattedMessage().contains(TASK), "the INFO line names the task");
            Assertions.assertFalse(infos.getFirst().getFormattedMessage().contains(PLAYER), "the INFO line names no player");
        }
    }

    @DisplayName("The stop logs one INFO line that names the task and the service")
    @Test
    void stopLogsAnInfoLineWithTheTaskAndService() {
        try (InfoLines log = new InfoLines(StagingIdle.class)) {
            stopAfterTheLimit(this.idle, this.services);

            List<ILoggingEvent> infos = log.infos();
            Assertions.assertEquals(1, infos.size(), "one accepted stop logs one INFO line");
            Assertions.assertTrue(infos.getFirst().getFormattedMessage().contains(TASK), "the INFO line names the task");
            Assertions.assertTrue(infos.getFirst().getFormattedMessage().contains(SERVICE), "the INFO line names the service");
        }
    }

    @DisplayName("No instrument carries a player's identifier, only the destination and the result")
    @Test
    void noInstrumentCarriesAPlayerIdentifier() {
        click();
        this.clock.advance(Duration.ofSeconds(90));
        refreshWaiting();
        click();
        this.services.serve(TASK, new ServiceState(SERVICE, Phase.RUNNING, 0, 10));
        stopAfterTheLimit(this.idle, this.services);

        for (MetricData metric : this.reader.collectAllMetrics()) {
            metric.getData().getPoints().forEach(point -> point.getAttributes().forEach((key, value) -> {
                Assertions.assertTrue(Set.of(DESTINATION, RESULT).contains(key), "instrument " + metric.getName() + " carries only destination and result, not " + key.getKey());
                Assertions.assertNotEquals(PLAYER, value, "instrument " + metric.getName() + " must never carry the player's identifier");
            }));
        }
    }

    private void click() {
        this.starts.click(PLAYER, STAGING, StagingStarts.read(this.services, STAGING));
    }

    private void refreshWaiting() {
        this.starts.refresh(Map.of(STAGING, StagingStarts.read(this.services, STAGING)));
    }

    /**
     * Serves a running service with nobody on it, then runs idle passes until the limit is reached.
     */
    private void stopAfterTheLimit(StagingIdle target, FakeOnDemandServices provider) {
        provider.serve(TASK, new ServiceState(SERVICE, Phase.RUNNING, 0, 10));
        target.refresh(Map.of(STAGING, StagingStarts.read(provider, STAGING)));
        this.clock.advance(LIMIT);
        target.refresh(Map.of(STAGING, StagingStarts.read(provider, STAGING)));
    }

    private static Attributes attributes(String result) {
        return Attributes.of(DESTINATION, STAGING.name(), RESULT, result);
    }

    private MetricData metric(String name) {
        List<MetricData> found = this.reader.collectAllMetrics().stream().filter(metric -> metric.getName().equals(name)).toList();
        Assertions.assertEquals(1, found.size(), "exactly one instrument named " + name + " must have recorded a value");
        return found.getFirst();
    }

    private long counter(String name, Attributes attributes) {
        return this.reader.collectAllMetrics().stream().filter(metric -> metric.getName().equals(name)).flatMap(metric -> metric.getLongSumData().getPoints().stream()).filter(point -> point.getAttributes().equals(attributes)).mapToLong(LongPointData::getValue).sum();
    }

    private HistogramPointData wait(String result) {
        List<HistogramPointData> points = metric(WAIT).getHistogramData().getPoints().stream().filter(point -> point.getAttributes().equals(attributes(result))).toList();
        Assertions.assertEquals(1, points.size(), "exactly one wait point for result " + result);
        return points.getFirst();
    }

    /** Stands in for both ports: every player may use the entry, and nothing is sent anywhere. */
    private static final class Ports implements StagingConnect<String>, StagingNotice<String> {

        @Override
        public boolean allowed(String player, Destination destination) {
            return true;
        }

        @Override
        public void connect(String player, Destination destination) {
            // The connection is not part of these measurements.
        }

        @Override
        public void waiting(String player, Destination destination) {
            // The action bar line is not part of these measurements.
        }

        @Override
        public void timeout(String player, Destination destination) {
            // The chat refusal is not part of these measurements.
        }

        @Override
        public void unavailable(String player) {
            // The refusal message is not part of these measurements.
        }
    }

    /** A provider whose stop is always refused, to measure the failed stop. */
    private static final class RefusingStops implements OnDemandServices {

        private final FakeOnDemandServices fake;

        RefusingStops(FakeOnDemandServices fake) {
            this.fake = fake;
        }

        @Override
        public Optional<List<ServiceState>> services(String task) {
            return this.fake.services(task);
        }

        @Override
        public boolean start(String task) {
            return this.fake.start(task);
        }

        @Override
        public boolean stop(String serviceName) {
            return false;
        }
    }

    /** The INFO lines one class logs while it is open, at INFO level for the duration. */
    private static final class InfoLines implements AutoCloseable {

        private final Logger logger;
        private final Level previous;
        private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

        InfoLines(Class<?> source) {
            this.logger = (Logger) LoggerFactory.getLogger(source);
            this.previous = this.logger.getLevel();
            this.logger.setLevel(Level.INFO);
            this.lines.start();
            this.logger.addAppender(this.lines);
        }

        List<ILoggingEvent> infos() {
            return this.lines.list.stream().filter(line -> line.getLevel() == Level.INFO).toList();
        }

        @Override
        public void close() {
            this.logger.detachAppender(this.lines);
            this.lines.stop();
            this.logger.setLevel(this.previous);
        }
    }
}
