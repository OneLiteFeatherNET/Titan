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

import net.onelitefeather.titan.common.utils.CloudNetEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tells the operator once at startup when the lobby runs under CloudNet but no server connector
 * was installed, so navigator transfers would silently do nothing.
 */
public final class ConnectorStartupCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStartupCheck.class);

    private ConnectorStartupCheck() {
    }

    public static void verify() {
        verify(CloudNetEnvironment.isPresent(), TitanServerConnector.isInstalled());
    }

    public static void verify(boolean cloudNetPresent, boolean connectorInstalled) {
        if (cloudNetPresent && !connectorInstalled) {
            LOGGER.error("CloudNet detected but no server connector installed - check the TitanCloudNetPermissions and CloudNet_Bridge " + "extensions in extensions/; navigator transfers will do nothing");
        }
    }
}
