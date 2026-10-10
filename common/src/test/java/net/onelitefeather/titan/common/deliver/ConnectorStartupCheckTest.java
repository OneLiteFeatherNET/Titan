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

import java.util.List;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit coverage for {@link ConnectorStartupCheck}; the CloudNet detection and the connector state
 * are passed in as booleans, so no filesystem or static holder is touched.
 */
class ConnectorStartupCheckTest {

    private static final String ERROR_LINE = "ERROR net.onelitefeather.titan.common.deliver.ConnectorStartupCheck - " + "CloudNet detected but no server connector installed - check the TitanCloudNetPermissions and CloudNet_Bridge " + "extensions in extensions/; navigator transfers will do nothing";

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @DisplayName("CloudNet running without an installed connector logs one ERROR naming the missing extensions")
    @Test
    void cloudNetWithoutConnectorLogsOneError() {
        ConnectorStartupCheck.verify(true, false);

        Assertions.assertEquals(List.of(ERROR_LINE), CapturingLoggerFactory.messages(), "exactly one ERROR naming TitanCloudNetPermissions and CloudNet_Bridge must be logged");
    }

    @DisplayName("Without CloudNet or with an installed connector the startup check logs nothing")
    @ParameterizedTest(name = "cloudNetPresent={0}, connectorInstalled={1}")
    @CsvSource({"false,false", "false,true", "true,true"})
    void otherCombinationsLogNothing(boolean cloudNetPresent, boolean connectorInstalled) {
        ConnectorStartupCheck.verify(cloudNetPresent, connectorInstalled);

        Assertions.assertTrue(CapturingLoggerFactory.messages().isEmpty(), "no log line expected for cloudNetPresent=" + cloudNetPresent + ", connectorInstalled=" + connectorInstalled);
    }
}
