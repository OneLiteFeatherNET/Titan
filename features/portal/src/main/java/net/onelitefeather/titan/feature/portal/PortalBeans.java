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

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.inject.Named;
import java.util.concurrent.Executor;

/** The portal module's own beans. */
@Factory
final class PortalBeans {

    static final String LABEL_READS = "portal-label-reads";

    @Bean
    PortalSettings settings() {
        return PortalSettings.read();
    }

    /** A virtual thread per read: a provider may block on the network, which a tick must not. */
    @Bean
    @Named(LABEL_READS)
    Executor labelReads() {
        return task -> Thread.ofVirtual().name("titan-portal-label-read").start(task);
    }
}
