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
package net.onelitefeather.titan.core.settings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A setting's name as Otis stores it, {@code namespace:value}, e.g. {@code titan:tag.enabled}.
 * The characters follow Otis' rules, so a bad name fails at startup instead of at the first save.
 */
public record SettingKey(String namespace, String value) {

    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VALUE = Pattern.compile("[a-z0-9_./-]+");

    public SettingKey {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Setting namespace must match [a-z0-9_.-]+, was '" + namespace + "'");
        }
        if (!VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Setting value must match [a-z0-9_./-]+, was '" + value + "'");
        }
    }

    /**
     * @param key {@code namespace:value}, split at the first colon
     * @throws IllegalArgumentException if the key has no colon or a part breaks Otis' rules
     */
    public static SettingKey parse(String key) {
        Objects.requireNonNull(key, "key must not be null");
        int separator = key.indexOf(':');
        if (separator < 0) {
            throw new IllegalArgumentException("Setting key must be namespace:value, was '" + key + "'");
        }
        return new SettingKey(key.substring(0, separator), key.substring(separator + 1));
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.value;
    }
}
