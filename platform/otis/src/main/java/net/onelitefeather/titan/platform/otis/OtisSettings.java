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
package net.onelitefeather.titan.platform.otis;

import io.avaje.config.Config;
import java.time.Duration;
import java.util.Objects;

/**
 * The {@code otis.*} connection keys. The token comes from {@code TITAN_OTIS_TOKEN} when that is
 * set, otherwise from {@code otis.token}; it is optional and never printed: {@link #toString()}
 * masks it.
 */
record OtisSettings(String url, String token, Duration connectTimeout, Duration requestTimeout,
                    int loadRetries, int circuitFailureThreshold, Duration circuitOpenFor) {

    static final String TOKEN_ENVIRONMENT_VARIABLE = "TITAN_OTIS_TOKEN";

    private static final String URL_KEY = "otis.url";
    private static final String TOKEN_KEY = "otis.token";
    private static final String CONNECT_TIMEOUT_KEY = "otis.connectTimeoutMillis";
    private static final String REQUEST_TIMEOUT_KEY = "otis.requestTimeoutMillis";
    private static final String LOAD_RETRIES_KEY = "otis.loadRetries";
    private static final String CIRCUIT_THRESHOLD_KEY = "otis.circuitFailureThreshold";
    private static final String CIRCUIT_OPEN_KEY = "otis.circuitOpenMillis";

    OtisSettings {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(token, "token must not be null");
        requirePositive(connectTimeout, CONNECT_TIMEOUT_KEY);
        requirePositive(requestTimeout, REQUEST_TIMEOUT_KEY);
        if (circuitOpenFor.isNegative()) {
            throw new IllegalArgumentException(CIRCUIT_OPEN_KEY + " must not be negative, was " + circuitOpenFor.toMillis());
        }
        if (loadRetries < 0) {
            throw new IllegalArgumentException(LOAD_RETRIES_KEY + " must not be negative, was " + loadRetries);
        }
        if (circuitFailureThreshold < 1) {
            throw new IllegalArgumentException(CIRCUIT_THRESHOLD_KEY + " must be at least 1, was " + circuitFailureThreshold);
        }
    }

    /**
     * The token that applies: the environment variable if it is set and not blank, else the config
     * value.
     */
    static String resolveToken(String fromEnvironment, String fromConfig) {
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.strip();
        }
        return fromConfig == null ? "" : fromConfig.strip();
    }

    /**
     * Reads the section from the configuration and the environment; an invalid value aborts the
     * start.
     */
    static OtisSettings read() {
        String url = Config.get(URL_KEY, "").strip();
        String token = resolveToken(System.getenv(TOKEN_ENVIRONMENT_VARIABLE), Config.get(TOKEN_KEY, ""));
        Duration connectTimeout = Duration.ofMillis(Config.getAs(CONNECT_TIMEOUT_KEY, Long::parseLong));
        Duration requestTimeout = Duration.ofMillis(Config.getAs(REQUEST_TIMEOUT_KEY, Long::parseLong));
        int loadRetries = Config.getAs(LOAD_RETRIES_KEY, Integer::parseInt);
        int threshold = Config.getAs(CIRCUIT_THRESHOLD_KEY, Integer::parseInt);
        Duration circuitOpenFor = Duration.ofMillis(Config.getAs(CIRCUIT_OPEN_KEY, Long::parseLong));
        return new OtisSettings(url, token, connectTimeout, requestTimeout, loadRetries, threshold, circuitOpenFor);
    }

    boolean hasToken() {
        return !this.token.isBlank();
    }

    private static void requirePositive(Duration duration, String key) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(key + " must be positive, was " + duration.toMillis());
        }
    }

    @Override
    public String toString() {
        return "OtisSettings[url=" + this.url + ", token=" + (hasToken() ? "****" : "unset") + ", connectTimeout=" + this.connectTimeout + ", requestTimeout=" + this.requestTimeout + ", loadRetries=" + this.loadRetries + ", circuitFailureThreshold=" + this.circuitFailureThreshold + ", circuitOpenFor=" + this.circuitOpenFor + "]";
    }
}
