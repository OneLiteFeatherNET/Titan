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
package net.onelitefeather.titan.bridge;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reads services through a CloudNet provider that is resolved on first use. Kept free of CloudNet
 * types so the "provider missing" behaviour is testable: a missing provider is reported once, not
 * on every lookup, and is looked up again next time in case it becomes available later.
 */
final class ServiceReadings {

    /** The CloudNet side: the readings of the services of a task, group or one service by name. */
    interface Source {
        List<ServiceReading> read(String type, String name);
    }

    private final Supplier<Source> resolver;
    private final Logger logger;
    private Source source;
    private boolean warned;

    /** @param resolver returns the source, or {@code null} while CloudNet does not provide it */
    ServiceReadings(Supplier<Source> resolver, Logger logger) {
        this.resolver = resolver;
        this.logger = logger;
    }

    synchronized List<ServiceReading> read(String type, String name) {
        if (source == null) {
            source = resolver.get();
            if (source == null) {
                if (!warned) {
                    warned = true;
                    logger.log(Level.WARNING, "CloudNet service provider is not available, portal labels show services as offline");
                }
                return List.of();
            }
            logger.log(Level.INFO, "CloudNet service provider resolved, portal labels read live player counts");
        }
        return source.read(type, name);
    }
}
