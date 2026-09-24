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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit coverage for {@link ExampleGreetingRule}'s pure logic - the base of the test pyramid
 * described in {@code docs/lobby-modules.md}: no {@link net.minestom.server.entity.Player}, no
 * {@code Env}, just inputs and outputs.
 */
class ExampleGreetingRuleTest {

    @DisplayName("isOnCooldown() is true while less time than the cooldown has passed")
    @Test
    void isOnCooldownIsTrueWhileWithinTheCooldownWindow() {
        Assertions.assertTrue(ExampleGreetingRule.isOnCooldown(1_000L, 1_500L, 1_000L));
    }

    @DisplayName("isOnCooldown() is false once exactly the cooldown has passed")
    @Test
    void isOnCooldownIsFalseOnceExactlyTheCooldownHasPassed() {
        Assertions.assertFalse(ExampleGreetingRule.isOnCooldown(1_000L, 2_000L, 1_000L));
    }

    @DisplayName("isOnCooldown() is false once more than the cooldown has passed")
    @Test
    void isOnCooldownIsFalseOnceMoreThanTheCooldownHasPassed() {
        Assertions.assertFalse(ExampleGreetingRule.isOnCooldown(1_000L, 5_000L, 1_000L));
    }

    @DisplayName("greeting() substitutes the player's name for the template's placeholder")
    @Test
    void greetingSubstitutesThePlayersNameForThePlaceholder() {
        Component greeting = ExampleGreetingRule.greeting("Welcome, %s!", "Notch");

        Assertions.assertEquals(Component.text("Welcome, Notch!"), greeting);
    }
}
