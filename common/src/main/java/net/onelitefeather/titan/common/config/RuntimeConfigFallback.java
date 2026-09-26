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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The shared helper behind every feature module's runtime fallback to a shipped configuration
 * default (see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 2):
 * loading the classpath {@code application.yaml}'s own values exactly once, and logging a
 * WARN line for an invalid runtime value exactly once per key and per distinct invalid value
 * seen for it - never once per read, which on a hot path (a tickle attack, a sit interaction, an
 * elytra boost, a spawn height check) would flood the log.
 *
 * <p>A module's own {@code *Settings} class reuses its already-existing, pure validation
 * functions (the ones {@code enable()} still runs once, unchanged, to abort startup on an
 * invalid value) and calls {@link #fallback(String, Object, String, Object)} only when
 * validation fails at runtime - see e.g. {@code TickleSettings#resolveCooldownMillis}. This
 * class knows nothing about any module's own keys or validation rules; it only loads the
 * shipped defaults and deduplicates the warning.
 *
 * <p>{@link #resolve(String, Object, Function, Supplier)} is the generic counterpart of
 * {@link #fallback(String, Object, String, Object)} a hot-path read (a tickle attack, a sit
 * interaction, an elytra boost, a spawn height check) resolves through instead: it evaluates
 * {@code shippedDefault} lazily - only once a live value turns out invalid, never on every call
 * - and remembers a persistently invalid raw value together with its resolved fallback, so it is
 * parsed and validated exactly once rather than on every read.
 *
 * <p>Instantiable, not a static-only utility, precisely so a test can build a fresh instance -
 * from a plain, in-memory {@link Configuration} (see the two-argument constructor) or from a
 * fixture classpath resource - instead of sharing the one production instance {@link #shared()}
 * hands out; see {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 3.1 ("Kein
 * Test ruft {@code Config}-Mutatoren auf").
 */
public final class RuntimeConfigFallback {

    private static final String DEFAULT_RESOURCE = "application.yaml";
    private static final Logger log = LoggerFactory.getLogger(RuntimeConfigFallback.class);

    private final Configuration shippedDefaults;
    private final ConcurrentMap<String, Object> lastWarnedInvalidValue = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CacheEntry<?>> invalidValueCache = new ConcurrentHashMap<>();

    /**
     * @param shippedDefaults the shipped configuration defaults this instance falls back to; a
     *                        test typically builds this directly (e.g.
     *                        {@code Configuration.builder().put(key, value).build()}) instead of
     *                        loading a real classpath resource
     */
    public RuntimeConfigFallback(Configuration shippedDefaults) {
        this.shippedDefaults = Objects.requireNonNull(shippedDefaults, "shippedDefaults must not be null");
    }

    /**
     * @param classpathResource the classpath resource to load the shipped defaults from, e.g.
     *                          {@code "application.yaml"}
     * @param classLoader       the class loader {@code classpathResource} is resolved against
     */
    public RuntimeConfigFallback(String classpathResource, ClassLoader classLoader) {
        this(ClasspathConfiguration.load(classpathResource, classLoader));
    }

    /**
     * @return the shipped configuration defaults this instance was built with, loaded exactly
     *         once - at construction, never again on a later call to this method
     */
    public Configuration shippedDefaults() {
        return this.shippedDefaults;
    }

    /**
     * Logs a WARN line naming {@code key}, {@code invalidValue} and {@code reason} - deduplicated
     * per {@code key}: the same {@code invalidValue} seen again for the same {@code key} does not
     * log a second time, but a different {@code invalidValue} for that same {@code key} does -
     * then returns {@code shippedDefaultValue} unchanged, so a call site can use this method
     * directly as its fallback expression.
     *
     * @param key                 the full configuration key the invalid value was read from, e.g.
     *                            {@code "tickle.cooldownMillis"}
     * @param invalidValue        the value that failed validation, as read (or as a whole, for a
     *                            setting with no single scalar, e.g. a list or a set of entries)
     * @param reason              why {@code invalidValue} was rejected, e.g. a caught exception's
     *                            own message
     * @param shippedDefaultValue the shipped default to fall back to and to return
     * @param <T>                 the value's type
     * @return {@code shippedDefaultValue}, unchanged
     */
    public <T> T fallback(String key, Object invalidValue, String reason, T shippedDefaultValue) {
        warnInvalid(key, invalidValue, reason, shippedDefaultValue);
        return shippedDefaultValue;
    }

    /**
     * Same deduplicated WARN line as {@link #fallback(String, Object, String, Object)}, without
     * returning a value - for a call site (e.g. the navigator's entries, which fall back as a
     * whole list rather than one substitutable scalar) that already has its own way of obtaining
     * the shipped default and only needs the logging, deduplicated the same way.
     *
     * @param key                 the full configuration key (or section) the invalid value was
     *                            read from
     * @param invalidValue        the value that failed validation
     * @param reason              why {@code invalidValue} was rejected
     * @param shippedDefaultValue the shipped default the caller is falling back to, named in the
     *                            log line only
     */
    public void warnInvalid(String key, Object invalidValue, String reason, Object shippedDefaultValue) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Object previousInvalidValue = this.lastWarnedInvalidValue.put(key, invalidValue);
        if (!Objects.equals(previousInvalidValue, invalidValue)) {
            log.warn("Invalid configuration value for {}: {} ({}), using shipped default {}", key, invalidValue, reason, shippedDefaultValue);
        }
    }

    /**
     * The generic runtime fallback every module's own {@code current()} (or
     * {@code currentXxx()}) method resolves one live value - or a small group of them,
     * cross-validated together - through, replacing a hand-written try/parseAndValidate/catch
     * per value: {@code shippedDefault} is evaluated lazily, only when {@code rawValue} turns out
     * invalid, never on a valid call, and a persistently invalid {@code rawValue} is remembered
     * per {@code key} together with the fallback it resolved to, so a repeated call with the same
     * invalid {@code rawValue} returns that cached fallback directly, without invoking
     * {@code parseAndValidate} again.
     *
     * <p>{@code rawValue} stands for whatever the live value(s) being resolved are, read fresh on
     * every call (this method never caches a <em>valid</em> value): a single raw string for one
     * key (e.g. {@code Config.get("tickle.cooldownMillis")}), a {@code List<String>} for a whole
     * list-valued key (e.g. {@code sit.allowedBlocks}), or a small record/map bundling the raw
     * values of a cross-validated group of keys (e.g. {@code spawn.minHeight} together with
     * {@code spawn.maxHeight}, since a valid {@code minHeight} depends on the already-resolved
     * {@code maxHeight}) - identified by one composite {@code key} the caller chooses for that
     * group. Whatever type is used, it must have a meaningful {@link Object#equals(Object)} - a
     * plain {@code String}, {@code List}, {@code Map} or {@code record} all qualify.
     *
     * <p>A new invalid {@code rawValue} for {@code key} logs a deduplicated WARN naming
     * {@code key}, {@code rawValue}, the failure's message and the shipped default used; the same
     * {@code rawValue} seen again for {@code key} does not log again, and a different, later
     * invalid {@code rawValue} for the same {@code key} logs again.
     *
     * @param key              the full configuration key (or, for a cross-validated group, a
     *                         composite name the caller chooses) {@code rawValue} was read from -
     *                         also the WARN-dedup and invalid-value-cache key
     * @param rawValue         the current live value(s), read fresh by the caller on every call
     * @param parseAndValidate parses and validates {@code rawValue}, throwing a
     *                         {@link RuntimeException} if it is invalid
     * @param shippedDefault   evaluated only when {@code parseAndValidate} throws - never for a
     *                         valid {@code rawValue}, and never twice for the same persistently
     *                         invalid one - to obtain the shipped classpath default to fall back
     *                         to
     * @param <R>              the raw value's type
     * @param <T>              the resolved value's type
     * @return {@code rawValue} parsed and validated, or the shipped default if it is invalid
     */
    public <R, T> T resolve(String key, R rawValue, Function<R, T> parseAndValidate, Supplier<T> shippedDefault) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(parseAndValidate, "parseAndValidate must not be null");
        Objects.requireNonNull(shippedDefault, "shippedDefault must not be null");

        CacheEntry<T> cached = this.<T>cacheEntry(key);
        if (cached != null && Objects.equals(cached.rawValue(), rawValue)) {
            return cached.fallbackValue();
        }

        try {
            T value = parseAndValidate.apply(rawValue);
            this.invalidValueCache.remove(key);
            return value;
        } catch (RuntimeException e) {
            T fallbackValue = shippedDefault.get();
            boolean isNewInvalidValue = cached == null || !Objects.equals(cached.rawValue(), rawValue);
            this.invalidValueCache.put(key, new CacheEntry<>(rawValue, fallbackValue));
            if (isNewInvalidValue) {
                log.warn("Invalid configuration value for {}: {} ({}), using shipped default {}", key, rawValue, e.getMessage(), fallbackValue);
            }
            return fallbackValue;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CacheEntry<T> cacheEntry(String key) {
        return (CacheEntry<T>) this.invalidValueCache.get(key);
    }

    /**
     * One key's (or key group's) last invalid raw value together with the fallback it resolved
     * to - so a persistently invalid value is parsed and validated exactly once.
     *
     * @param rawValue      the invalid raw value(s) this entry was cached for
     * @param fallbackValue the shipped default {@code rawValue} resolved to
     * @param <T>           the resolved value's type
     */
    private record CacheEntry<T>(Object rawValue, T fallbackValue) {
    }

    /**
     * The lazily-initialized, process-wide instance every module's production code reads through -
     * see the initialization-on-demand holder idiom this relies on for "loaded exactly once,
     * however many modules and read points ask for it".
     */
    private static final class Holder {

        private static final RuntimeConfigFallback INSTANCE = new RuntimeConfigFallback(DEFAULT_RESOURCE, RuntimeConfigFallback.class.getClassLoader());

        private Holder() {
        }
    }

    /**
     * @return the shared, process-wide instance backing every module's live configuration reads -
     *         never a fresh one, so its shipped defaults are loaded once for the whole process and
     *         its warning-dedupe state is shared across every module and every read point
     */
    public static RuntimeConfigFallback shared() {
        return Holder.INSTANCE;
    }
}
