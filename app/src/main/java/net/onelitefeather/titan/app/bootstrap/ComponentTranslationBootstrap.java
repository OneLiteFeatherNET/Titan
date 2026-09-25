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
package net.onelitefeather.titan.app.bootstrap;

import java.util.function.Function;

/**
 * Turns on Minestom's built-in, per-receiver component translation - {@code
 * ServerFlag.AUTOMATIC_COMPONENT_TRANSLATION} - unless an operator already set it.
 *
 * <p>{@code ServerFlag}'s fields are {@code static final}, read once from system properties in its
 * static initialiser ({@code net.minestom.server.ServerFlag#AUTOMATIC_COMPONENT_TRANSLATION},
 * property {@value #PROPERTY_NAME}, default {@code false}). A value set any later - even before
 * {@code MinecraftServer.init()} - is too late once that class has been touched once. {@link
 * #enableComponentTranslation()} must therefore run as the very first statement of {@code
 * TitanApplication#main}, before anything else in the process (including
 * {@code TitanObservability.bootstrap()}) can load a Minestom class.
 *
 * <p>Without this flag, Minestom never renders a
 * {@link net.kyori.adventure.text.TranslatableComponent}
 * through {@link net.kyori.adventure.translation.GlobalTranslator} at all - not for an outgoing
 * packet to a player, and not for the console's own flattener
 * ({@code net.minestom.server.adventure.provider.MinestomFlattenerProvider}, verified by
 * decompiling
 * {@code minestom-2026.06.05-26.1.2.jar}): a translatable component is silently dropped instead,
 * which is why {@code /titanreload}'s reply used to render as an empty line on the console. See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 5.
 *
 * <p><b>Cost.</b> Enabling this makes Minestom render a packet containing a translatable
 * component per receiver instead of once, bypassing its shared packet cache for that packet -
 * negligible today, since the only translatable components in the lobby are admin command
 * replies (e.g. {@code /titanreload}'s), sent to at most a handful of receivers. It would matter
 * if translatable components were ever broadcast at high frequency to many players at once - a
 * tab list, a boss bar, a scoreboard - where the shared cache is what keeps that broadcast cheap.
 *
 * <p><b>Testability.</b> {@link #valueToSet(Function)} is the pure decision - given the current
 * value of {@value #PROPERTY_NAME} (or {@code null} if unset), the value the property must be set
 * to, or {@code null} to leave it alone - pulled apart from the actual
 * {@code System.getProperty}/{@code System.setProperty} calls so a test can supply a fake lookup (a
 * plain {@link java.util.Map}) instead of mutating the real, process-wide system properties
 * (F.I.R.S.T. - Independent/Repeatable).
 */
public final class ComponentTranslationBootstrap {

    /** The system property {@code net.minestom.server.ServerFlag} reads at class-init time. */
    public static final String PROPERTY_NAME = "minestom.automatic-component-translation";

    private ComponentTranslationBootstrap() {
    }

    /**
     * Sets {@value #PROPERTY_NAME} to {@code true} unless it is already set - production entry
     * point, operating on the real system properties. Must run before any Minestom class is
     * touched (see the class javadoc).
     */
    public static void enableComponentTranslation() {
        String value = valueToSet(System::getProperty);
        if (value != null) {
            System.setProperty(PROPERTY_NAME, value);
        }
    }

    /**
     * @param properties looks up a system property's current value by name, {@code null} if unset -
     *                   production passes {@link System#getProperty(String)}, a test passes a
     *                   plain {@link java.util.Map}'s {@code get}
     * @return {@code "true"} if {@value #PROPERTY_NAME} is unset and must be enabled, {@code null}
     *         if an operator already set a value and it must be left untouched
     */
    static String valueToSet(Function<String, String> properties) {
        return properties.apply(PROPERTY_NAME) == null ? "true" : null;
    }
}
