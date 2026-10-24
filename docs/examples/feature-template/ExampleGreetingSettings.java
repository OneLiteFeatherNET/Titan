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
package net.onelitefeather.titan.apps.cloudnet.feature.example;

/**
 * Pure validation for the {@code example} module's two config values. A real module pairs a
 * function like this with a read at the edge of its own {@code start()}, e.g.
 *
 * <pre>{@code
 * String greeting = ExampleGreetingSettings.greeting(Config.get(GREETING_KEY));
 * long cooldownMillis = Config.getAs(COOLDOWN_KEY, ExampleGreetingSettings::cooldownMillis);
 * }</pre>
 *
 * <p>This template has no section to read, so {@link ExampleModule#start} validates its hardcoded
 * defaults instead - the snippet above is what a real module would write.
 */
final class ExampleGreetingSettings {

    /** The full key a real module would read {@link #greeting(String)}'s input from. */
    static final String GREETING_KEY = "example.greeting";

    /** The full key a real module would read {@link #cooldownMillis(String)}'s input from. */
    static final String COOLDOWN_KEY = "example.cooldownMillis";

    private ExampleGreetingSettings() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    static String greeting(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("must not be blank");
        }
        if (!raw.contains("%s")) {
            throw new IllegalArgumentException("must contain a '%s' placeholder for the player's name");
        }
        return raw;
    }

    /**
     * Used as {@code Config.getAs}'s mapping function; {@code getAs} wraps any thrown exception,
     * naming {@link #COOLDOWN_KEY} itself.
     */
    static long cooldownMillis(String raw) {
        long millis = Long.parseLong(raw);
        if (millis < 0) {
            throw new IllegalArgumentException("must not be negative, was " + millis);
        }
        return millis;
    }
}
