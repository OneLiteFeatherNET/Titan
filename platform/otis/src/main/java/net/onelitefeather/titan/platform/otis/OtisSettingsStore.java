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

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingValue;
import net.onelitefeather.titan.core.settings.SettingsStore;
import net.onelitefeather.titan.core.settings.SettingsStoreException;
import net.onelitefeather.titan.core.settings.SettingsStoreException.Failure;
import net.onelitefeather.titan.core.settings.SettingsTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Otis store: {@code GET /v1/players/{uuid}/settings?namespace=titan} to load and
 * {@code PUT /v1/players/{uuid}/settings/{key}} to save, over {@link HttpClient}. A circuit breaker
 * guards every call, and the bearer token is sent only when one is configured.
 *
 * <p>Failures carry the status and the operation, never the token, the value or the response body.
 * Retries of a write are left to the caller, which owns the queue; a load is retried here.
 */
final class OtisSettingsStore implements SettingsStore {

    private static final Logger LOG = LoggerFactory.getLogger(OtisSettingsStore.class);
    private static final String NAMESPACE = "titan";

    private final String baseUrl;
    private final OtisSettings settings;
    private final HttpClient http;
    private final CircuitBreaker breaker;

    OtisSettingsStore(OtisSettings settings, HttpClient http, CircuitBreaker breaker) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.baseUrl = stripTrailingSlash(settings.url());
        this.http = Objects.requireNonNull(http, "http must not be null");
        this.breaker = Objects.requireNonNull(breaker, "breaker must not be null");
    }

    /**
     * The store the lobby runs with: its own HTTP client and a breaker that reports to telemetry.
     */
    static OtisSettingsStore create(OtisSettings settings, Clock clock, SettingsTelemetry telemetry) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).version(HttpClient.Version.HTTP_1_1).build();
        CircuitBreaker breaker = new CircuitBreaker(settings.circuitFailureThreshold(), settings.circuitOpenFor(), clock, state -> {
            telemetry.circuitChanged(state.telemetryName());
            if (state == CircuitBreaker.State.OPEN) {
                LOG.warn("Otis is not reached: the settings circuit is open, changes stay in memory");
            }
        });
        return new OtisSettingsStore(settings, http, breaker);
    }

    @Override
    public Map<SettingKey, SettingValue> load(UUID player) {
        for (int attempt = 0;; attempt++) {
            try {
                return loadOnce(player);
            } catch (SettingsStoreException failure) {
                boolean retryable = failure.failure() == Failure.TIMEOUT || failure.failure() == Failure.UNAVAILABLE;
                if (!retryable || attempt >= this.settings.loadRetries()) {
                    throw failure;
                }
            }
        }
    }

    @Override
    public void save(UUID player, SettingKey key, SettingValue value) {
        URI uri = URI.create(this.baseUrl + "/v1/players/" + player + "/settings/" + key);
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(uri)).header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(OtisJson.valueBody(value))).build();
        HttpResponse<String> response = exchange(request, "save");
        int status = response.statusCode();
        if (status / 100 != 2) {
            throw failureFor(status, "save");
        }
    }

    private Map<SettingKey, SettingValue> loadOnce(UUID player) {
        URI uri = URI.create(this.baseUrl + "/v1/players/" + player + "/settings?namespace=" + NAMESPACE);
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(uri)).GET().build();
        HttpResponse<String> response = exchange(request, "load");
        int status = response.statusCode();
        if (status == 404) {
            // Otis does not know the player: nothing is stored for them yet.
            return Map.of();
        }
        if (status / 100 != 2) {
            throw failureFor(status, "load");
        }
        try {
            return OtisJson.parseSettings(response.body());
        } catch (JsonParseException | IllegalStateException malformed) {
            throw new SettingsStoreException(Failure.UNAVAILABLE, "Otis answered a settings load with a malformed body", malformed);
        }
    }

    private HttpRequest.Builder withAuthorization(HttpRequest.Builder builder) {
        builder.timeout(this.settings.requestTimeout()).header("Accept", "application/json");
        if (this.settings.hasToken()) {
            builder.header("Authorization", "Bearer " + this.settings.token());
        }
        return builder;
    }

    // The breaker sees every call: a transport failure or an error status counts, a 404 is an answer.
    private HttpResponse<String> exchange(HttpRequest request, String operation) {
        if (!this.breaker.tryAcquire()) {
            throw new SettingsStoreException(Failure.CIRCUIT_OPEN, "Otis is not reached: the circuit is open for a " + operation);
        }
        HttpResponse<String> response;
        try {
            response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException timeout) {
            this.breaker.recordFailure();
            throw new SettingsStoreException(Failure.TIMEOUT, "Otis did not answer a settings " + operation + " in time", timeout);
        } catch (IOException network) {
            this.breaker.recordFailure();
            throw new SettingsStoreException(Failure.UNAVAILABLE, "Otis could not be reached for a settings " + operation, network);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            this.breaker.recordFailure();
            throw new SettingsStoreException(Failure.UNAVAILABLE, "interrupted while calling Otis for a settings " + operation, interrupted);
        }
        int status = response.statusCode();
        if (status == 404 || status / 100 == 2) {
            this.breaker.recordSuccess();
        } else {
            this.breaker.recordFailure();
        }
        return response;
    }

    private static SettingsStoreException failureFor(int status, String operation) {
        Failure failure;
        if (status == 404) {
            failure = Failure.NOT_FOUND;
        } else if (status == 409 || status >= 500) {
            failure = Failure.UNAVAILABLE;
        } else {
            failure = Failure.REJECTED;
        }
        return new SettingsStoreException(failure, "Otis answered HTTP " + status + " to a settings " + operation);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
