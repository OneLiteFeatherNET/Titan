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
package net.onelitefeather.titan.runtime.bootstrap;

import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.runtime.Titan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the single lifecycle line {@link Titan#Titan()} emits once the active
 * {@link PermissionService} is resolved from the built {@code BeanScope}, pulled out on its own so
 * it can be unit-tested with a captured appender.
 */
public final class PermissionStartupLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionStartupLog.class);

    private PermissionStartupLog() {
    }

    /** Logs {@code permissionService.name()} as a single INFO line. */
    public static void activeService(PermissionService permissionService) {
        LOGGER.info("Permissions resolved by {}", permissionService.name());
    }
}
