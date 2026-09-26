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
package net.onelitefeather.titan.app.feature.tickle;

import io.avaje.config.Config;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;

/**
 * Pure parsing and validation for the {@code tickle} module's configuration value (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decisions 3 and 4).
 *
 * <p>{@link #cooldownMillis(String)} is used directly as the mapping function of
 * {@code Config.getAs(COOLDOWN_KEY, TickleSettings::cooldownMillis)} in
 * {@link TickleModule#enable}, once, to abort startup on an invalid value (unchanged behaviour
 * from {@code avaje-config-facade}): it never touches {@code io.avaje.config.Config} itself, so
 * it is unit-testable on its own, and {@code getAs} wraps any exception it throws into an
 * {@code IllegalStateException} that names {@link #COOLDOWN_KEY} once and keeps this method's own
 * exception as the cause (key in the message, reason in the cause chain - verified against
 * avaje-config 5.2's {@code CoreConfiguration#getAs}).
 *
 * <p>{@link #resolveCooldownMillis(String, long, RuntimeConfigFallback)} is the pure runtime
 * counterpart (see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 2):
 * built on top of {@link #cooldownMillis(String)} rather than duplicating its rule, it falls back
 * to the shipped default - deduplicating the WARN through the given
 * {@link RuntimeConfigFallback} - instead of throwing. {@link #current()} is the one place that
 * reads the live value through the facade, used by {@link TickleAttackHandler} on every attack.
 */
final class TickleSettings {

    /** The full key {@link #cooldownMillis(String)} reads and validates. */
    static final String COOLDOWN_KEY = "tickle.cooldownMillis";

    private TickleSettings() {
    }

    /**
     * Parses and validates the tickle cooldown.
     *
     * @param raw the configured cooldown in milliseconds, as text; must parse as a whole number
     *            that is not negative
     * @return {@code raw}, parsed
     * @throws NumberFormatException    if {@code raw} does not parse as a {@code long}
     * @throws IllegalArgumentException if the parsed value is negative
     */
    static long cooldownMillis(String raw) {
        long millis = Long.parseLong(raw);
        if (millis < 0) {
            throw new IllegalArgumentException("must not be negative, was " + millis);
        }
        return millis;
    }

    /**
     * The runtime counterpart of {@link #cooldownMillis(String)}: a valid {@code raw} passes
     * through unchanged; an invalid one - a non-numeric value or a negative one - falls back to
     * {@code shippedDefault} instead of throwing, after {@code fallback} logs a deduplicated WARN
     * naming {@link #COOLDOWN_KEY}, the offending {@code raw} value and the reason.
     *
     * @param raw            the configured cooldown, as read live from the facade
     * @param shippedDefault the shipped classpath default for {@link #COOLDOWN_KEY}
     * @param fallback       deduplicates the WARN line for a repeated invalid value
     * @return {@code raw}, parsed and validated, or {@code shippedDefault} if invalid
     */
    static long resolveCooldownMillis(String raw, long shippedDefault, RuntimeConfigFallback fallback) {
        try {
            return cooldownMillis(raw);
        } catch (RuntimeException e) {
            return fallback.fallback(COOLDOWN_KEY, raw, e.getMessage(), shippedDefault);
        }
    }

    /**
     * Reads {@link #COOLDOWN_KEY} live through the static facade, resolving an invalid runtime
     * value to the shipped default via the process-wide {@link RuntimeConfigFallback}. Called
     * directly by {@link TickleAttackHandler} on every attack (see design.md, decision 1: a pull,
     * not a push) rather than once in {@link TickleModule#enable}, so a changed value applies to
     * the very next attack without a module restart.
     *
     * @return the current, valid cooldown in milliseconds
     */
    static long current() {
        RuntimeConfigFallback fallback = RuntimeConfigFallback.shared();
        long shippedDefault = fallback.shippedDefaults().getAs(COOLDOWN_KEY, Long::parseLong);
        return resolveCooldownMillis(Config.get(COOLDOWN_KEY), shippedDefault, fallback);
    }
}
