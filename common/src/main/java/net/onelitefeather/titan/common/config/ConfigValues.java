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

import io.avaje.config.Config;

/**
 * Reads a numeric value from the {@code io.avaje.config.Config} static facade, translating a
 * {@link NumberFormatException} into a {@link ConfigException} that names the full key - something
 * {@code Config.getInt}/{@code Config.getLong} do not do on their own (see design.md, decision 4).
 *
 * <p>A module reads a numeric value with {@link #intValue(String)}, {@link #longValue(String)} or
 * {@link #doubleValue(String)} in its own {@code enable()} - the one place a module is allowed to
 * touch the facade at all (see design.md, decision 3). Everything else ({@code String}, list and
 * boolean values) is read directly with {@code Config.get}, {@code Config.list().of} and
 * {@code Config.getBool}, which already carry the key in their own failure messages.
 *
 * <p>{@link #parseInt(String, String)}, {@link #parseLong(String, String)} and
 * {@link #parseDouble(String, String)} are the pure parsing logic behind those three methods,
 * package-private so a test exercises them directly against a raw string, without ever touching
 * {@link Config} (design.md, decision 5: no unit test calls the facade).
 */
public final class ConfigValues {

    private ConfigValues() {
    }

    /**
     * @param key the full configuration key, e.g. {@code "tickle.cooldownMillis"}
     * @return the key's value, parsed as an {@code int}
     * @throws ConfigException if the value is missing or not a whole number in the {@code int}
     *                         range
     */
    public static int intValue(String key) {
        return parseInt(key, Config.get(key));
    }

    /**
     * @param key the full configuration key, e.g. {@code "tickle.cooldownMillis"}
     * @return the key's value, parsed as a {@code long}
     * @throws ConfigException if the value is missing or not a whole number in the {@code long}
     *                         range
     */
    public static long longValue(String key) {
        return parseLong(key, Config.get(key));
    }

    /**
     * @param key the full configuration key, e.g. {@code "sit.offset.x"}
     * @return the key's value, parsed as a {@code double}
     * @throws ConfigException if the value is missing or not a number
     */
    public static double doubleValue(String key) {
        return parseDouble(key, Config.get(key));
    }

    /**
     * @param key the full configuration key the value {@code raw} was read from, used only for the
     *            {@link ConfigException} message
     * @param raw the raw string value to parse
     * @return {@code raw}, parsed as an {@code int}
     * @throws ConfigException if {@code raw} is not a whole number in the {@code int} range
     */
    static int parseInt(String key, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw ConfigException.invalid(key, "must be a whole number, was '" + raw + "'");
        }
    }

    /**
     * @param key the full configuration key the value {@code raw} was read from, used only for the
     *            {@link ConfigException} message
     * @param raw the raw string value to parse
     * @return {@code raw}, parsed as a {@code long}
     * @throws ConfigException if {@code raw} is not a whole number in the {@code long} range
     */
    static long parseLong(String key, String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw ConfigException.invalid(key, "must be a whole number, was '" + raw + "'");
        }
    }

    /**
     * @param key the full configuration key the value {@code raw} was read from, used only for the
     *            {@link ConfigException} message
     * @param raw the raw string value to parse
     * @return {@code raw}, parsed as a {@code double}
     * @throws ConfigException if {@code raw} is not a number
     */
    static double parseDouble(String key, String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw ConfigException.invalid(key, "must be a number, was '" + raw + "'");
        }
    }
}
