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
package net.onelitefeather.titan.common.deliver;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for the static {@link TitanServerConnector} holder. The bridge is replaced by a
 * recording {@link ServerConnector} fake; the holder is reset around every test.
 */
class TitanServerConnectorTest {

    private static final UUID PLAYER_ID = UUID.fromString("6f1c2a52-0d3e-4b8a-9c77-3e5f1a2b4c6d");

    @BeforeEach
    void clearStateBeforeTest() {
        TitanServerConnector.setConnector(null);
        CapturingLoggerFactory.clear();
    }

    @AfterEach
    void uninstallConnector() {
        TitanServerConnector.setConnector(null);
    }

    @DisplayName("Without an installed connector the holder reports that none is installed")
    @Test
    void isInstalledIsFalseWithoutConnector() {
        Assertions.assertFalse(TitanServerConnector.isInstalled(), "no connector is installed by default");
    }

    @DisplayName("With an installed connector the holder reports that one is installed")
    @Test
    void isInstalledIsTrueWithConnector() {
        TitanServerConnector.setConnector(new RecordingConnector());

        Assertions.assertTrue(TitanServerConnector.isInstalled(), "an installed connector must be reported");
    }

    @DisplayName("A task transfer is handed to the installed connector and reports true")
    @Test
    void connectToTaskReportsHandedOverWhenConnectorInstalled() {
        RecordingConnector connector = new RecordingConnector();
        TitanServerConnector.setConnector(connector);

        boolean handedOver = TitanServerConnector.connectToTask(PLAYER_ID, "cygnus");

        Assertions.assertTrue(handedOver, "the transfer must be reported as handed over");
        Assertions.assertEquals(List.of("task " + PLAYER_ID + " cygnus"), connector.calls, "the connector must receive the task transfer");
        Assertions.assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a handed-over transfer must not log");
    }

    @DisplayName("A task transfer without a connector reports false")
    @Test
    void connectToTaskReportsMissedWithoutConnector() {
        boolean handedOver = TitanServerConnector.connectToTask(PLAYER_ID, "cygnus");

        Assertions.assertFalse(handedOver, "a transfer without a connector must be reported as missed");
    }

    @DisplayName("A server transfer is handed to the installed connector and reports true")
    @Test
    void connectToServerReportsHandedOverWhenConnectorInstalled() {
        RecordingConnector connector = new RecordingConnector();
        TitanServerConnector.setConnector(connector);

        boolean handedOver = TitanServerConnector.connectToServer(PLAYER_ID, "survival-1");

        Assertions.assertTrue(handedOver, "the transfer must be reported as handed over");
        Assertions.assertEquals(List.of("server " + PLAYER_ID + " survival-1"), connector.calls, "the connector must receive the server transfer");
        Assertions.assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a handed-over transfer must not log");
    }

    @DisplayName("A server transfer without a connector reports false")
    @Test
    void connectToServerReportsMissedWithoutConnector() {
        boolean handedOver = TitanServerConnector.connectToServer(PLAYER_ID, "survival-1");

        Assertions.assertFalse(handedOver, "a transfer without a connector must be reported as missed");
    }

    private static final class RecordingConnector implements ServerConnector {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void connectToTask(UUID playerId, String taskName) {
            this.calls.add("task " + playerId + " " + taskName);
        }

        @Override
        public void connectToServer(UUID playerId, String serviceName) {
            this.calls.add("server " + playerId + " " + serviceName);
        }
    }
}
