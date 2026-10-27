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

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingValue;
import net.onelitefeather.titan.core.settings.SettingsStoreException;
import net.onelitefeather.titan.core.settings.SettingsStoreException.Failure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The adapter against a loopback stand-in for Otis: paths, headers, bodies, retries and the
 * breaker.
 */
class OtisSettingsStoreIntegrationTest {

    private static final SettingKey TAG = SettingKey.parse("titan:tag.enabled");
    private static final SettingKey VOLUME = SettingKey.parse("titan:radio.volume");
    private static final UUID PLAYER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final String STORED = "[{\"key\":\"titan:tag.enabled\",\"value\":true,\"version\":2,\"updatedAt\":\"2026-10-24T12:00:00Z\"}]";

    private static OtisSettings settings(String url, String token, int loadRetries, int threshold, Duration requestTimeout) {
        return new OtisSettings(url, token, Duration.ofSeconds(1), requestTimeout, loadRetries, threshold, Duration.ofSeconds(30));
    }

    private static OtisSettingsStore store(OtisSettings settings) {
        CircuitBreaker breaker = new CircuitBreaker(settings.circuitFailureThreshold(), settings.circuitOpenFor(), Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC), state -> {
        });
        return new OtisSettingsStore(settings, HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build(), breaker);
    }

    @DisplayName("A load returns the stored values and asks for the titan namespace")
    @Test
    void loadReturnsStoredValues() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.of(200, STORED))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 0, 3, Duration.ofSeconds(2)));
            Assertions.assertEquals(Map.of(TAG, new SettingValue.Flag(true)), store.load(PLAYER), "the stored value must be returned");
            FakeOtis.Received request = otis.received().getFirst();
            Assertions.assertEquals("/v1/players/" + PLAYER + "/settings", request.path(), "the load must address the player's settings");
            Assertions.assertEquals("namespace=titan", request.query(), "the load must ask for the titan namespace only");
        }
    }

    @DisplayName("A player Otis does not know has no stored values")
    @Test
    void unknownPlayerLoadsAsEmpty() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.empty(404))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 0, 3, Duration.ofSeconds(2)));
            Assertions.assertTrue(store.load(PLAYER).isEmpty(), "a 404 on load means nothing is stored for the player");
        }
    }

    @DisplayName("A server error on load is retried once")
    @Test
    void loadRetriesAfterAServerError() throws IOException {
        try (FakeOtis otis = new FakeOtis().then(FakeOtis.Reply.empty(500)).always(FakeOtis.Reply.of(200, STORED))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 1, 3, Duration.ofSeconds(2)));
            Assertions.assertEquals(Map.of(TAG, new SettingValue.Flag(true)), store.load(PLAYER), "the retry must deliver the stored value");
            Assertions.assertEquals(2, otis.received().size(), "one failed request and one retry");
        }
    }

    @DisplayName("A load gives up after its retry and reports the server error")
    @Test
    void loadGivesUpAfterTheRetry() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.empty(500))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 1, 5, Duration.ofSeconds(2)));
            SettingsStoreException failure = Assertions.assertThrows(SettingsStoreException.class, () -> store.load(PLAYER), "a failing load must surface");
            Assertions.assertEquals(Failure.UNAVAILABLE, failure.failure(), "a server error must be reported as unavailable");
            Assertions.assertEquals(2, otis.received().size(), "the load must try once more and then stop");
        }
    }

    @DisplayName("A load that Otis does not answer in time fails with a timeout")
    @Test
    void slowOtisTimesOut() throws IOException {
        CountDownLatch hang = new CountDownLatch(1);
        try (FakeOtis otis = new FakeOtis().always(new FakeOtis.Reply(200, STORED, hang))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 0, 3, Duration.ofMillis(200)));
            try {
                SettingsStoreException failure = Assertions.assertThrows(SettingsStoreException.class, () -> store.load(PLAYER), "a load past the request timeout must fail");
                Assertions.assertEquals(Failure.TIMEOUT, failure.failure(), "a request past its timeout must be a timeout");
            } finally {
                hang.countDown();
            }
        }
    }

    @DisplayName("The bearer token is sent when one is configured")
    @Test
    void bearerTokenIsSent() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.of(200, "[]"))) {
            store(settings(otis.url(), "tok-123", 0, 3, Duration.ofSeconds(2))).load(PLAYER);
            Assertions.assertEquals("Bearer tok-123", otis.received().getFirst().authorization(), "the configured token must be sent as a bearer token");
        }
    }

    @DisplayName("No Authorization header is sent without a token")
    @Test
    void noHeaderWithoutToken() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.of(200, "[]"))) {
            store(settings(otis.url(), "", 0, 3, Duration.ofSeconds(2))).load(PLAYER);
            Assertions.assertNull(otis.received().getFirst().authorization(), "without a token the lobby must call without the header");
        }
    }

    @DisplayName("A save puts the value as a JSON object under the key's path")
    @Test
    void saveWritesTheValueBody() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.of(201, "{}"))) {
            store(settings(otis.url(), "", 0, 3, Duration.ofSeconds(2))).save(PLAYER, TAG, new SettingValue.Flag(true));
            FakeOtis.Received request = otis.received().getFirst();
            Assertions.assertEquals("PUT", request.method(), "a save must be a PUT");
            Assertions.assertEquals("/v1/players/" + PLAYER + "/settings/titan:tag.enabled", request.path(), "the key must be in the path");
            Assertions.assertEquals("{\"value\":true}", request.body(), "the body must carry the value");
        }
    }

    @DisplayName("A save of a level puts its number as the value")
    @Test
    void saveWritesTheLevelAsANumber() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.of(201, "{}"))) {
            store(settings(otis.url(), "", 0, 3, Duration.ofSeconds(2))).save(PLAYER, VOLUME, new SettingValue.Level(75));
            FakeOtis.Received request = otis.received().getFirst();
            Assertions.assertEquals("/v1/players/" + PLAYER + "/settings/titan:radio.volume", request.path(), "the level key must be in the path");
            Assertions.assertEquals("{\"value\":75}", request.body(), "a level must be sent as a JSON number");
        }
    }

    @DisplayName("A load returns a stored whole number as a level")
    @Test
    void loadReturnsStoredLevel() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.of(200, "[{\"key\":\"titan:radio.volume\",\"value\":75,\"version\":1,\"updatedAt\":\"2026-10-24T12:00:00Z\"}]"))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 0, 3, Duration.ofSeconds(2)));

            Assertions.assertEquals(Map.of(VOLUME, new SettingValue.Level(75)), store.load(PLAYER), "the stored number must come back as a level");
        }
    }

    @DisplayName("A rejected save is not retryable")
    @Test
    void rejectedSaveIsNotRetryable() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.empty(400))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 0, 5, Duration.ofSeconds(2)));
            SettingsStoreException failure = Assertions.assertThrows(SettingsStoreException.class, () -> store.save(PLAYER, TAG, new SettingValue.Flag(true)));
            Assertions.assertEquals(Failure.REJECTED, failure.failure(), "a 400 must be reported as rejected");
            Assertions.assertFalse(failure.failure().retryable(), "a rejected write must not be retried");
        }
    }

    @DisplayName("A save answered with 404 is reported as not found and may be retried later")
    @Test
    void saveForUnknownPlayerIsNotFound() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.empty(404))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 0, 5, Duration.ofSeconds(2)));
            SettingsStoreException failure = Assertions.assertThrows(SettingsStoreException.class, () -> store.save(PLAYER, TAG, new SettingValue.Flag(true)));
            Assertions.assertEquals(Failure.NOT_FOUND, failure.failure(), "a 404 on save must be reported as not found");
            Assertions.assertTrue(failure.failure().retryable(), "a not-found write may succeed later");
        }
    }

    @DisplayName("Repeated failures open the breaker, and an open breaker sends nothing")
    @Test
    void openBreakerSendsNothing() throws IOException {
        try (FakeOtis otis = new FakeOtis().always(FakeOtis.Reply.empty(503))) {
            OtisSettingsStore store = store(settings(otis.url(), "", 0, 2, Duration.ofSeconds(2)));
            Assertions.assertThrows(SettingsStoreException.class, () -> store.save(PLAYER, TAG, new SettingValue.Flag(true)));
            Assertions.assertThrows(SettingsStoreException.class, () -> store.save(PLAYER, TAG, new SettingValue.Flag(true)));
            SettingsStoreException refused = Assertions.assertThrows(SettingsStoreException.class, () -> store.save(PLAYER, TAG, new SettingValue.Flag(true)));
            Assertions.assertEquals(Failure.CIRCUIT_OPEN, refused.failure(), "the third call must be refused by the open breaker");
            Assertions.assertEquals(2, otis.received().size(), "a refused call must not reach Otis");
        }
    }
}
