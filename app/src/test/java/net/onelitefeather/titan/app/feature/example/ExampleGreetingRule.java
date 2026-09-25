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

import net.kyori.adventure.text.Component;

/**
 * The {@code example} module's pure decision and formatting logic, kept apart from
 * {@link ExampleModule} and {@link ExampleGreetingTracker} so it can be unit-tested without a
 * {@link net.minestom.server.entity.Player} or an {@code Env} - the base of the test pyramid, see
 * {@code docs/lobby-modules.md}.
 *
 * <p>Package-private: no other feature touches this directly, see {@code design.md}, decision 9
 * ("Tags/Items gehören dem Feature") - the same reasoning applies to a feature's own pure logic.
 */
final class ExampleGreetingRule {

    private ExampleGreetingRule() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * @param lastGreetedAtMillis when the player was last greeted, as returned by
     *                            {@link java.time.Clock#millis()}
     * @param nowMillis           the current time, as returned by {@link java.time.Clock#millis()}
     * @param cooldownMillis      how long a player must wait between greetings
     * @return {@code true} if fewer than {@code cooldownMillis} have passed since
     *         {@code lastGreetedAtMillis}
     */
    static boolean isOnCooldown(long lastGreetedAtMillis, long nowMillis, long cooldownMillis) {
        return nowMillis - lastGreetedAtMillis < cooldownMillis;
    }

    /**
     * @param template   a greeting template (see {@link ExampleGreetingSettings#greeting(String)}),
     *                   containing exactly one {@code %s} placeholder
     * @param playerName the greeted player's name, substituted for {@code %s}
     * @return the formatted greeting
     */
    static Component greeting(String template, String playerName) {
        return Component.text(String.format(template, playerName));
    }
}
