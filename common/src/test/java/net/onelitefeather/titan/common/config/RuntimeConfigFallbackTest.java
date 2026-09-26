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
package net.onelitefeather.titan.common.config;

import io.avaje.config.Configuration;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link RuntimeConfigFallback} (see
 * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 3.1): a new invalid value
 * warns once, the same invalid value again does not, a different invalid value for the same key
 * warns again, and the shipped defaults are loaded exactly once. Every test builds its own, fresh
 * instance (F.I.R.S.T. - Independent): none of this shares state with another test, and none of
 * it calls {@code io.avaje.config.Config}.
 *
 * <p>Log lines are asserted through {@link CapturingLoggerFactory}, the SLF4J test binding
 * registered for the {@code common} module's test sources - see {@code
 * net.onelitefeather.titan.common.deliver.DebugDeliverTest}'s Javadoc for why this, rather than
 * logback's {@code ListAppender}, is the right tool here.
 */
class RuntimeConfigFallbackTest {

    private static final String KEY = "tickle.cooldownMillis";

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    private static RuntimeConfigFallback fallbackWithShippedDefault(long shippedDefault) {
        Configuration shipped = Configuration.builder().put(KEY, Long.toString(shippedDefault)).build();
        return new RuntimeConfigFallback(shipped);
    }

    @DisplayName("A new invalid value returns the shipped default and warns once")
    @Test
    void newInvalidValueWarnsOnce() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);

        long result = fallback.fallback(KEY, "-5", "must not be negative, was -5", 4000L);

        Assertions.assertEquals(4000L, result, "the shipped default must be returned");
        Assertions.assertEquals(1, CapturingLoggerFactory.messages().size(), "exactly one WARN must be logged");
        String logged = CapturingLoggerFactory.messages().getFirst();
        Assertions.assertTrue(logged.startsWith("WARN "), "must log at WARN, was: " + logged);
        Assertions.assertTrue(logged.contains(KEY), "must name the key, was: " + logged);
        Assertions.assertTrue(logged.contains("-5"), "must name the invalid value, was: " + logged);
        Assertions.assertTrue(logged.contains("must not be negative"), "must name the reason, was: " + logged);
        Assertions.assertTrue(logged.contains("4000"), "must name the shipped default, was: " + logged);
    }

    @DisplayName("The same invalid value seen again does not warn a second time")
    @Test
    void sameInvalidValueDoesNotWarnAgain() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);

        fallback.fallback(KEY, "-5", "must not be negative, was -5", 4000L);
        fallback.fallback(KEY, "-5", "must not be negative, was -5", 4000L);
        fallback.fallback(KEY, "-5", "must not be negative, was -5", 4000L);

        Assertions.assertEquals(1, CapturingLoggerFactory.messages().size(), "a repeated, unchanged invalid value must warn only once");
    }

    @DisplayName("A different invalid value for the same key warns again")
    @Test
    void differentInvalidValueForSameKeyWarnsAgain() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);

        fallback.fallback(KEY, "-5", "must not be negative, was -5", 4000L);
        fallback.fallback(KEY, "-9", "must not be negative, was -9", 4000L);

        Assertions.assertEquals(2, CapturingLoggerFactory.messages().size(), "a newly seen, different invalid value for the same key must warn again");
    }

    @DisplayName("warnInvalid deduplicates exactly like fallback, without returning a value")
    @Test
    void warnInvalidDeduplicatesTheSameWay() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);

        fallback.warnInvalid("navigator.entries", "bad-reason", "bad-reason", List.of("shipped-entry"));
        fallback.warnInvalid("navigator.entries", "bad-reason", "bad-reason", List.of("shipped-entry"));

        Assertions.assertEquals(1, CapturingLoggerFactory.messages().size(), "warnInvalid must dedupe exactly like fallback");
    }

    private static long cooldownMillis(String raw) {
        long millis = Long.parseLong(raw);
        if (millis < 0) {
            throw new IllegalArgumentException("must not be negative, was " + millis);
        }
        return millis;
    }

    @DisplayName("resolve passes a valid raw value through unchanged, without ever evaluating the shipped default")
    @Test
    void resolveValidRawValueNeverEvaluatesTheShippedDefault() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);
        AtomicInteger shippedDefaultEvaluations = new AtomicInteger();

        long result = fallback.resolve(KEY, "1500", RuntimeConfigFallbackTest::cooldownMillis, () -> {
            shippedDefaultEvaluations.incrementAndGet();
            return 4000L;
        });

        Assertions.assertEquals(1500L, result, "a valid raw value must pass through unchanged");
        Assertions.assertEquals(0, shippedDefaultEvaluations.get(), "the shipped default must never be evaluated for a valid raw value");
        Assertions.assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a valid raw value must never warn");
    }

    @DisplayName("resolve returns the shipped default and warns once for a new invalid raw value")
    @Test
    void resolveNewInvalidValueWarnsOnce() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);

        long result = fallback.resolve(KEY, "-5", RuntimeConfigFallbackTest::cooldownMillis, () -> 4000L);

        Assertions.assertEquals(4000L, result, "the shipped default must be returned");
        Assertions.assertEquals(1, CapturingLoggerFactory.messages().size(), "exactly one WARN must be logged");
        String logged = CapturingLoggerFactory.messages().getFirst();
        Assertions.assertTrue(logged.startsWith("WARN "), "must log at WARN, was: " + logged);
        Assertions.assertTrue(logged.contains(KEY), "must name the key, was: " + logged);
        Assertions.assertTrue(logged.contains("-5"), "must name the invalid raw value, was: " + logged);
        Assertions.assertTrue(logged.contains("must not be negative"), "must name the reason, was: " + logged);
        Assertions.assertTrue(logged.contains("4000"), "must name the shipped default, was: " + logged);
    }

    @DisplayName("resolve parses a persistently invalid raw value exactly once and does not warn again")
    @Test
    void resolvePersistentlyInvalidValueParsesOnlyOnce() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);
        AtomicInteger parserInvocations = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            long result = fallback.resolve(KEY, "-5", raw -> {
                parserInvocations.incrementAndGet();
                return cooldownMillis(raw);
            }, () -> 4000L);
            Assertions.assertEquals(4000L, result, "every call must return the cached shipped default");
        }

        Assertions.assertEquals(1, parserInvocations.get(), "the persistently invalid raw value must be parsed exactly once");
        Assertions.assertEquals(1, CapturingLoggerFactory.messages().size(), "a repeated, unchanged invalid raw value must warn only once");
    }

    @DisplayName("resolve parses a different invalid raw value for the same key again and warns again")
    @Test
    void resolveDifferentInvalidValueForSameKeyWarnsAgain() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);
        AtomicInteger parserInvocations = new AtomicInteger();

        fallback.resolve(KEY, "-5", raw -> {
            parserInvocations.incrementAndGet();
            return cooldownMillis(raw);
        }, () -> 4000L);
        fallback.resolve(KEY, "-9", raw -> {
            parserInvocations.incrementAndGet();
            return cooldownMillis(raw);
        }, () -> 4000L);

        Assertions.assertEquals(2, parserInvocations.get(), "a newly seen, different invalid raw value must be parsed again");
        Assertions.assertEquals(2, CapturingLoggerFactory.messages().size(), "a newly seen, different invalid raw value for the same key must warn again");
    }

    @DisplayName("resolve recovers on a valid raw value seen after an invalid one, dropping the cached entry")
    @Test
    void resolveValidValueAfterInvalidRecovers() {
        RuntimeConfigFallback fallback = fallbackWithShippedDefault(4000L);

        long invalidResult = fallback.resolve(KEY, "-5", RuntimeConfigFallbackTest::cooldownMillis, () -> 4000L);
        long recoveredResult = fallback.resolve(KEY, "1500", RuntimeConfigFallbackTest::cooldownMillis, () -> 4000L);
        AtomicInteger parserInvocations = new AtomicInteger();
        long resultAfterSameInvalidAgain = fallback.resolve(KEY, "-5", raw -> {
            parserInvocations.incrementAndGet();
            return cooldownMillis(raw);
        }, () -> 4000L);

        Assertions.assertEquals(4000L, invalidResult);
        Assertions.assertEquals(1500L, recoveredResult, "a valid raw value must recover, not reuse the cached fallback");
        Assertions.assertEquals(4000L, resultAfterSameInvalidAgain, "the same invalid value seen again after a recovery must warn again");
        Assertions.assertEquals(1, parserInvocations.get(), "the recovery must have dropped the stale cached entry, so this is parsed again");
        Assertions.assertEquals(2, CapturingLoggerFactory.messages().size(), "the invalid value seen again after a recovery must warn again");
    }

    @DisplayName("The shipped defaults are loaded exactly once, at construction")
    @Test
    void shippedDefaultsAreLoadedOnlyOnce() {
        AtomicInteger resourceReads = new AtomicInteger();
        ClassLoader countingClassLoader = new ClassLoader(RuntimeConfigFallbackTest.class.getClassLoader()) {

            @Override
            public InputStream getResourceAsStream(String name) {
                resourceReads.incrementAndGet();
                String yaml = "tickle:\n  cooldownMillis: 4000\n";
                return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            }
        };

        RuntimeConfigFallback fallback = new RuntimeConfigFallback("application.yaml", countingClassLoader);
        int readsAfterConstruction = resourceReads.get();

        Configuration first = fallback.shippedDefaults();
        Configuration second = fallback.shippedDefaults();
        Configuration third = fallback.shippedDefaults();

        Assertions.assertSame(first, second, "shippedDefaults() must return the very same instance every time");
        Assertions.assertSame(second, third, "shippedDefaults() must return the very same instance every time");
        Assertions.assertEquals(readsAfterConstruction, resourceReads.get(), "reading shippedDefaults() again must not re-read the classpath resource");
        Assertions.assertTrue(readsAfterConstruction > 0, "constructing the instance must have read the resource at least once");
    }
}
