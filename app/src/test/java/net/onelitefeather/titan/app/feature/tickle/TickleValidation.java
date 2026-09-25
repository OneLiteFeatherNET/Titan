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

import net.onelitefeather.titan.common.config.ConfigException;

/**
 * Test-only entry point into {@link TickleModule#readCooldown()} - the exact read-and-validate
 * line {@link TickleModule#enable} runs for {@code tickle.cooldownMillis} - without building a
 * {@link TickleModule} or needing a Minestom server: {@link
 * net.onelitefeather.titan.app.bootstrap.ConfigurationPrintMain} calls {@link #validate()} from a
 * child JVM so {@link net.onelitefeather.titan.app.bootstrap.ConfigurationPrecedenceTest} can
 * cover the {@code lobby-module-config} spec's "Ungültiger Override" and "Negative Dauer"
 * scenarios for this module - {@link TickleModule#readCooldown()} is package-private, hence this
 * helper living in the same package rather than {@code ConfigurationPrintMain} calling it
 * directly.
 *
 * <p>Public, unlike {@link TickleModule#readCooldown()} itself, purely so a class in another
 * package/module (the bootstrap test package) can call it; it adds no validation of its own.
 */
public final class TickleValidation {

    private TickleValidation() {
    }

    /**
     * Runs {@link TickleModule#readCooldown()} - the same read-and-validate line
     * {@link TickleModule#enable} runs for {@code tickle.cooldownMillis}.
     *
     * @throws ConfigException if the configured value is missing, not a whole number, or negative
     */
    public static void validate() {
        TickleModule.readCooldown();
    }
}
