package net.onelitefeather.titan.common.deliver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for the static {@link TitanServiceControl} holder. The CloudNet side is replaced
 * by a recording {@link ServiceControl} fake; the holder is reset around every test.
 */
class TitanServiceControlTest {

    private static final String TASK = "Slender-Staging";
    private static final Map<String, Object> ROW = Map.of(ServiceControl.NAME, "Slender-Staging-1", ServiceControl.PHASE, "RUNNING", ServiceControl.ONLINE, 3, ServiceControl.MAX, 20);

    @BeforeEach
    void clearStateBeforeTest() {
        TitanServiceControl.setControl(null);
        CapturingLoggerFactory.clear();
    }

    @AfterEach
    void uninstallControl() {
        TitanServiceControl.setControl(null);
    }

    @DisplayName("Without an installed control the holder reports that none is installed")
    @Test
    void isInstalledIsFalseWithoutControl() {
        Assertions.assertFalse(TitanServiceControl.isInstalled(), "no control is installed by default");
    }

    @DisplayName("Without an installed control no task can be read, so the read is empty and not an empty list")
    @Test
    void readIsEmptyWithoutControl() {
        Assertions.assertEquals(Optional.empty(), TitanServiceControl.servicesByTask(TASK), "an unreadable task must not look like a task that runs nowhere");
    }

    @DisplayName("Without an installed control a start is refused")
    @Test
    void startIsRefusedWithoutControl() {
        Assertions.assertFalse(TitanServiceControl.startTask(TASK), "no control means no service is started");
    }

    @DisplayName("Without an installed control a stop is refused")
    @Test
    void stopIsRefusedWithoutControl() {
        Assertions.assertFalse(TitanServiceControl.stopService("Slender-Staging-1"), "no control means nothing is stopped");
    }

    @DisplayName("The rows of the installed control are handed through unchanged")
    @Test
    void readHandsRowsThrough() {
        FakeControl control = new FakeControl(List.of(ROW));
        TitanServiceControl.setControl(control);

        Assertions.assertEquals(Optional.of(List.of(ROW)), TitanServiceControl.servicesByTask(TASK), "the rows of the control");
        Assertions.assertEquals(List.of(TASK), control.reads, "the control receives the task");
    }

    @DisplayName("A start is handed to the installed control and its answer is returned")
    @Test
    void startHandsTaskThrough() {
        FakeControl control = new FakeControl(List.of());
        TitanServiceControl.setControl(control);

        Assertions.assertTrue(TitanServiceControl.startTask(TASK), "the control accepted the start");
        Assertions.assertEquals(List.of(TASK), control.starts, "the control receives the task");
    }

    @DisplayName("A stop is handed to the installed control by service name")
    @Test
    void stopHandsNameThrough() {
        FakeControl control = new FakeControl(List.of());
        TitanServiceControl.setControl(control);

        Assertions.assertTrue(TitanServiceControl.stopService("Slender-Staging-1"), "the control accepted the stop");
        Assertions.assertEquals(List.of("Slender-Staging-1"), control.stops, "the control receives the service name");
    }

    @DisplayName("A failing read is reported as unreadable and logged once as a warning")
    @Test
    void failingReadIsEmptyAndWarned() {
        TitanServiceControl.setControl(new FakeControl(List.of()).failing());

        Assertions.assertEquals(Optional.empty(), TitanServiceControl.servicesByTask(TASK), "a failed read is unreadable");
        Assertions.assertEquals(1, CapturingLoggerFactory.messages().stream().filter(line -> line.startsWith("WARN ")).count(), "exactly one warning for the failed read");
    }

    @DisplayName("A failing start is refused instead of thrown into the caller")
    @Test
    void failingStartIsRefused() {
        TitanServiceControl.setControl(new FakeControl(List.of()).failing());

        Assertions.assertFalse(TitanServiceControl.startTask(TASK), "a failed start is refused");
    }

    /** Records every call; {@link #failing()} makes each call throw as a broken CloudNet would. */
    private static final class FakeControl implements ServiceControl {

        private final List<Map<String, Object>> rows;
        private final List<String> reads = new ArrayList<>();
        private final List<String> starts = new ArrayList<>();
        private final List<String> stops = new ArrayList<>();
        private boolean failing;

        FakeControl(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        FakeControl failing() {
            this.failing = true;
            return this;
        }

        @Override
        public List<Map<String, Object>> servicesByTask(String task) {
            this.reads.add(task);
            if (this.failing) {
                throw new IllegalStateException("broken");
            }
            return this.rows;
        }

        @Override
        public boolean start(String task) {
            this.starts.add(task);
            if (this.failing) {
                throw new IllegalStateException("broken");
            }
            return true;
        }

        @Override
        public boolean stop(String serviceName) {
            this.stops.add(serviceName);
            return true;
        }
    }
}
