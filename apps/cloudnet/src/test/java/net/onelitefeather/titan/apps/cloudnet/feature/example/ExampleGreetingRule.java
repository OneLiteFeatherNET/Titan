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

import net.kyori.adventure.text.Component;

/**
 * The {@code example} module's pure decision and formatting logic, kept apart from
 * {@link ExampleModule} so it is unit-testable without a {@link net.minestom.server.entity.Player}.
 *
 * <p>Package-private: a feature's pure logic belongs to that feature alone.
 */
final class ExampleGreetingRule {

    private ExampleGreetingRule() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    static boolean isOnCooldown(long lastGreetedAtMillis, long nowMillis, long cooldownMillis) {
        return nowMillis - lastGreetedAtMillis < cooldownMillis;
    }

    static Component greeting(String template, String playerName) {
        return Component.text(String.format(template, playerName));
    }
}
