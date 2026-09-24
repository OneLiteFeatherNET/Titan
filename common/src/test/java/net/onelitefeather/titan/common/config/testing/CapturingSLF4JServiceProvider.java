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
package net.onelitefeather.titan.common.config.testing;

import org.slf4j.IMarkerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * A tiny SLF4J provider, registered for the {@code common} test sources via {@code
 * META-INF/services/org.slf4j.spi.SLF4JServiceProvider}, that records every log line through
 * {@link CapturingLoggerFactory} instead of discarding it. Without this, SLF4J falls back to a
 * no-op logger during tests (there is no logging binding on the test runtime classpath), which
 * makes it impossible to assert that {@link
 * net.onelitefeather.titan.common.config.LegacyConfigMigration} actually logs the legacy keys it
 * drops.
 */
public final class CapturingSLF4JServiceProvider implements SLF4JServiceProvider {

    private static final CapturingLoggerFactory LOGGER_FACTORY = new CapturingLoggerFactory();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return LOGGER_FACTORY;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return new BasicMarkerFactory();
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return new NOPMDCAdapter();
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.99";
    }

    @Override
    public void initialize() {
        // Nothing to do.
    }
}
