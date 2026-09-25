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
package net.onelitefeather.titan.app.feature.example;

/**
 * Pure validation for the {@code example} module's two values - the "prüfen in reinen Funktionen"
 * half of the pattern described in {@code docs/lobby-modules.md}, "Konfiguration lesen" (design.md,
 * decision 3). A real module pairs a function like this with a read at the edge of its own
 * {@code enable()}, e.g.
 *
 * <pre>{@code
 * String greeting = ExampleGreetingSettings.greeting(Config.get(GREETING_KEY));
 * long cooldownMillis = Config.getAs(COOLDOWN_KEY, ExampleGreetingSettings::cooldownMillis);
 * }</pre>
 *
 * <p>This template has no section of its own in the shipped {@code application.yaml} (see
 * {@link ExampleModule}'s class Javadoc for why), so {@link ExampleModule#enable} validates its own
 * hardcoded defaults instead of performing that read - the snippet above is what a real module with
 * a real section would write in its place.
 *
 * <p>Package-private, with its own unit test ({@code ExampleGreetingSettingsTest}) that never
 * touches {@code Config} (design.md, decision 5): a pure function like this takes a value and
 * either returns it or throws {@link IllegalArgumentException}, so it is testable with plain inputs
 * and outputs.
 */
final class ExampleGreetingSettings {

    /** The full key a real module would read {@link #greeting(String)}'s input from. */
    static final String GREETING_KEY = "example.greeting";

    /** The full key a real module would read {@link #cooldownMillis(String)}'s input from. */
    static final String COOLDOWN_KEY = "example.cooldownMillis";

    private ExampleGreetingSettings() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * @param raw the greeting template, as read from {@link #GREETING_KEY}
     * @return {@code raw}, unchanged
     * @throws IllegalArgumentException if {@code raw} is blank, or does not contain a {@code %s}
     *                                  placeholder for the player's name
     */
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
     * Parses and validates the cooldown, in milliseconds. Used directly as the mapping function of
     * {@code Config.getAs(COOLDOWN_KEY, ExampleGreetingSettings::cooldownMillis)} - {@code getAs}
     * wraps any exception it throws into an {@code IllegalStateException} naming
     * {@link #COOLDOWN_KEY} once, keeping this method's own exception as the cause.
     *
     * @param raw the cooldown in milliseconds, as text
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
}
