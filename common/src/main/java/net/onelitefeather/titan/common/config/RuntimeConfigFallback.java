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
