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
package net.onelitefeather.titan.feature.portal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.SourceType;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Time passes only through the scheduler's own tick methods; every test builds its own scheduler
 * and telemetry.
 */
class LabelRefreshTest {

    private static final int PERIOD_TICKS = 20;

    private final Scheduler scheduler = Scheduler.newScheduler();
    private final Logger refreshLogger = (Logger) LoggerFactory.getLogger(LabelRefresh.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();
    private final TestTelemetry testTelemetry = TestTelemetry.create();
    private final PortalTelemetry telemetry = new PortalTelemetry(this.testTelemetry.telemetry());

    @BeforeEach
    void captureLog() {
        this.lines.start();
        this.refreshLogger.addAppender(this.lines);
    }

    @AfterEach
    void releaseLog() {
        this.refreshLogger.detachAppender(this.lines);
        this.lines.stop();
        this.testTelemetry.close();
    }

    private static LabelRefresh.Entry entry(String id, List<String> shown) {
        PortalLabel label = new PortalLabel(new Vec(0, 64, 0), "<online>", null, null, Billboard.CENTER, 0f);
        Portal portal = new Portal(id, new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "Survival", null, label);
        LabelDisplay display = new LabelDisplay(text -> shown.add(PlainTextComponentSerializer.plainText().serialize(text)), () -> {
        });
        return new LabelRefresh.Entry(portal, label, display);
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.scheduler.processTick();
            this.scheduler.process();
        }
    }

    private List<ILoggingEvent> warnings() {
        return this.lines.list.stream().filter(line -> line.getLevel() == Level.WARN).toList();
    }

    private LabelRefresh refresh(Executor executor, LabelReadings readings, LabelRefresh.Entry... entries) {
        return new LabelRefresh(this.scheduler, executor, readings, List.of(entries), this.telemetry);
    }

    @DisplayName("An executor that rejects the read is offered the read again next period and warns once")
    @Test
    void rejectingExecutorIsRetried() {
        AtomicInteger offered = new AtomicInteger();
        List<String> shown = new ArrayList<>();
        Executor rejectingTwice = task -> {
            if (offered.incrementAndGet() <= 2) {
                throw new RejectedExecutionException("pool is shut down");
            }
            task.run();
        };
        LabelRefresh refresh = refresh(rejectingTwice, portal -> new LabelReading.Local(4), entry("a", shown));
        refresh.start(1);

        tick(2);
        tick(PERIOD_TICKS);
        assertEquals(2, offered.get(), "the flag was reset, so the next period offered the read again");
        assertEquals(1, warnings().size(), "the same failure is a warning once");
        assertEquals(List.of(), shown, "nothing was read yet");

        tick(PERIOD_TICKS);
        tick(1);
        assertEquals(List.of("4"), shown, "the third offer ran and was applied");
        refresh.stop();
    }

    @DisplayName("A result that arrives after stop() is not applied")
    @Test
    void lateResultAfterStopIsIgnored() {
        QueuedExecutor executor = new QueuedExecutor();
        List<String> shown = new ArrayList<>();
        LabelRefresh refresh = refresh(executor, portal -> new LabelReading.Local(4), entry("a", shown));
        refresh.start(1);
        tick(2);
        assertEquals(1, executor.pending(), "a read is under way");

        executor.runAll();
        refresh.stop();
        tick(2);

        assertEquals(List.of(), shown, "the display is being removed, it must not be touched any more");
    }

    @DisplayName("A label whose read throws keeps its text and does not drop the others; the failure warns once")
    @Test
    void failingEntryDoesNotDropTheOthers() {
        List<String> shownA = new ArrayList<>();
        List<String> shownB = new ArrayList<>();
        LabelReadings readings = portal -> {
            if (portal.id().equals("a")) {
                throw new IllegalStateException("provider down");
            }
            return new LabelReading.Local(5);
        };
        LabelRefresh refresh = refresh(Runnable::run, readings, entry("a", shownA), entry("b", shownB));
        refresh.start(1);

        tick(2);
        tick(PERIOD_TICKS);
        tick(PERIOD_TICKS);

        assertEquals(List.of(), shownA, "the failing label keeps what it showed");
        assertEquals(List.of("5"), shownB, "the healthy label is applied, an unchanged count is not sent again");
        assertEquals(1, warnings().size(), "three failed reads, one warning");
        refresh.stop();
    }

    @DisplayName("A refresh cycle is one span with the label count and the number of failed reads")
    @Test
    void aCycleIsSpannedWithCountAndFailures() {
        LabelReadings readings = portal -> {
            if (portal.id().equals("a")) {
                throw new IllegalStateException("provider down");
            }
            return new LabelReading.Local(5);
        };
        LabelRefresh refresh = refresh(Runnable::run, readings, entry("a", new ArrayList<>()), entry("b", new ArrayList<>()));
        refresh.start(1);

        tick(2);
        refresh.stop();

        SpanData span = this.testTelemetry.span("portal.labels.refresh");
        assertEquals(2L, this.testTelemetry.attribute(span, AttributeKey.longKey("portal.labels.count")), "both labels are in the cycle");
        assertEquals(1L, this.testTelemetry.attribute(span, AttributeKey.longKey("portal.labels.failed")), "one read failed");
    }

    @DisplayName("Each player-count read counts once: a successful one as ok, a throwing one as error")
    @Test
    void lookupsAreCountedByResult() {
        LabelReadings readings = portal -> {
            if (portal.id().equals("a")) {
                throw new IllegalStateException("provider down");
            }
            return new LabelReading.Local(5);
        };
        LabelRefresh refresh = refresh(Runnable::run, readings, entry("a", new ArrayList<>()), entry("b", new ArrayList<>()));
        refresh.start(1);

        tick(2);
        refresh.stop();

        assertEquals(1, this.testTelemetry.counter("titan.portal.player_count.lookups", Attributes.of(AttributeKey.stringKey("result"), "error")), "the throwing lookup");
        assertEquals(1, this.testTelemetry.counter("titan.portal.player_count.lookups", Attributes.of(AttributeKey.stringKey("result"), "ok")), "the healthy lookup");
    }

    @DisplayName("A provider that throws shows the label as not running and counts the failed lookup and the span")
    @Test
    void throwingProviderIsCountedAsFailedLookup() {
        PlayerCounts throwing = new PlayerCounts() {
            @Override
            public boolean supports(SourceType type) {
                return true;
            }

            @Override
            public PlayerCount count(SourceType type, String name) {
                throw new IllegalStateException("cloud unreachable");
            }
        };
        List<String> shown = new ArrayList<>();
        LabelRefresh.Entry entry = entry("a", shown);
        LabelRefresh refresh = refresh(Runnable::run, new LabelReader(throwing, () -> 0), entry);
        refresh.start(1);

        tick(2);
        refresh.stop();

        String notRunning = PlainTextComponentSerializer.plainText().serialize(LabelRenderer.render(entry.portal(), entry.label(), new LabelReading.Remote(PlayerCount.NOT_RUNNING)));
        assertEquals(List.of(notRunning), shown, "the label shows the not running state, as before");
        assertEquals(1, this.testTelemetry.counter("titan.portal.player_count.lookups", Attributes.of(AttributeKey.stringKey("result"), "error")), "the failed provider call is an error lookup");
        assertEquals(1L, this.testTelemetry.attribute(this.testTelemetry.span("portal.labels.refresh"), AttributeKey.longKey("portal.labels.failed")), "the refresh span counts the failed read");
    }

    @DisplayName("A period whose ticks overflow an int is rejected instead of wrapping")
    @Test
    void overflowingPeriodIsRejected() {
        LabelRefresh refresh = refresh(Runnable::run, portal -> new LabelReading.Local(1));

        assertThrows(ArithmeticException.class, () -> refresh.start(Integer.MAX_VALUE), "seconds times ticks per second must not wrap");
    }

    @DisplayName("A start after stop() schedules nothing")
    @Test
    void startAfterStopSchedulesNothing() {
        AtomicInteger offered = new AtomicInteger();
        LabelRefresh refresh = refresh(task -> offered.incrementAndGet(), portal -> new LabelReading.Local(1), entry("a", new ArrayList<>()));
        refresh.stop();

        refresh.start(1);
        tick(2 * PERIOD_TICKS);

        assertEquals(0, offered.get(), "no refresh may run after stop");
    }

    @DisplayName("A failure that persists warns once; a failure after a recovery warns again")
    @Test
    void warnsAgainAfterRecovery() {
        AtomicBoolean failing = new AtomicBoolean(true);
        LabelReadings readings = portal -> {
            if (failing.get()) {
                throw new IllegalStateException("provider down");
            }
            return new LabelReading.Local(5);
        };
        LabelRefresh refresh = refresh(Runnable::run, readings, entry("a", new ArrayList<>()));
        refresh.start(1);

        tick(2);
        tick(PERIOD_TICKS);
        assertEquals(1, warnings().size(), "two failed reads, one warning");

        failing.set(false);
        tick(PERIOD_TICKS);
        failing.set(true);
        tick(PERIOD_TICKS);

        assertEquals(2, warnings().size(), "the failure after the recovery is reported again");
        assertEquals("Reading the label of portal 'a' failed, keeping its displayed text: java.lang.IllegalStateException: provider down", warnings().getLast().getFormattedMessage(), "text with the exception type");
        refresh.stop();
    }
}
