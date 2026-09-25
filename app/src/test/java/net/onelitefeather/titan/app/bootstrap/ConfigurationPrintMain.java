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

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import java.util.Set;
import net.onelitefeather.titan.app.feature.navigator.NavigatorValidation;
import net.onelitefeather.titan.app.feature.tickle.TickleValidation;
import net.onelitefeather.titan.common.config.ConfigException;
import net.onelitefeather.titan.common.config.ConfigurationFactory;

/**
 * The child process entry point {@link ConfigurationPrecedenceTest} launches: runs the exact same
 * {@link ConfigurationFactory#initialise()} step production runs, through the very same static
 * {@link Config} facade {@code PlatformBeans}/{@code Titan} use, then either prints one
 * {@code key=value} line per requested key to stdout - {@code <absent>} if the key resolves to
 * nothing at all - or, for the two special arguments below, runs the same read-and-validate path a
 * module runs at startup and reports the outcome. Either way, the parent test process - which
 * cannot reach into this JVM's memory - asserts on what this process printed.
 *
 * <p>Four modes, chosen by {@code args[0]}:
 * <ul>
 * <li>{@value #VALIDATE_TICKLE}: runs {@link TickleValidation#validate()} - the same
 * {@code Config.getAs(key, Long::parseLong)}/{@code TickleSettings.cooldown} line
 * {@code TickleModule.enable} runs. Prints {@code tickle=OK} and exits {@code 0} if the configured
 * value is valid; otherwise prints {@code ERROR: <message>}, naming the full key once, plus one
 * {@code Caused by: <cause message>} line per exception in the cause chain (the reason, e.g. a
 * {@link NumberFormatException}'s own message, so both the key and the reason reach this
 * process's stdout even though {@code Config.getAs} wraps the parse failure into an
 * {@link IllegalStateException} rather than a {@link ConfigException}), and exits {@code 1}.</li>
 * <li>{@value #NAVIGATOR_ENTRIES}: runs {@link NavigatorValidation#resolvedEntryNames()} - the same
 * name resolution {@code NavigatorModule.enable} runs for {@code navigator.entries} - and prints
 * {@code navigator.entries=<name>,<name>,...}.</li>
 * <li>{@value #LOG_ACTIVE_PROFILES}: runs {@link ConfigurationStartupLog#activeProfiles()} - the
 * exact call {@code Titan}'s constructor makes right after
 * {@link ConfigurationFactory#initialise()}
 * - so {@link net.onelitefeather.titan.app.bootstrap.ConfigurationPrecedenceTest} can assert on the
 * INFO line it logs under a chosen profile. The line reaches this process's stdout via the
 * {@code CONSOLE} appender {@code common/src/main/resources/logback.xml} wires to {@code root}, the
 * same file production runs with, so the parent test can read it back merged with this process's
 * regular output (see {@link ConfigurationPrecedenceTest#startAndWait}).</li>
 * <li>anything else: every argument is a configuration key to print, in order, via
 * {@code Configuration.get(key, "<absent>")} - the original, plain read mode.</li>
 * </ul>
 * Neither validation mode touches a Minestom server: both helpers stop short of anything that
 * needs Minestom's registry data (see their own Javadoc).
 *
 * <p>{@link ConfigurationPrecedenceTest} controls this process's working directory, environment and
 * system properties via {@link ProcessBuilder} before launching it, so what this class prints is
 * exactly what a real lobby process would resolve, or reject, under the same conditions.
 */
public final class ConfigurationPrintMain {

    private static final String VALIDATE_TICKLE = "--validate-tickle";
    private static final String NAVIGATOR_ENTRIES = "--navigator-entries";
    private static final String LOG_ACTIVE_PROFILES = "--log-active-profiles";

    private ConfigurationPrintMain() {
    }

    public static void main(String[] args) {
        new ConfigurationFactory().initialise();

        if (args.length == 1 && VALIDATE_TICKLE.equals(args[0])) {
            validateTickle();
            return;
        }
        if (args.length == 1 && NAVIGATOR_ENTRIES.equals(args[0])) {
            printNavigatorEntries();
            return;
        }
        if (args.length == 1 && LOG_ACTIVE_PROFILES.equals(args[0])) {
            ConfigurationStartupLog.activeProfiles();
            return;
        }

        Configuration configuration = Config.asConfiguration();
        for (String key : args) {
            System.out.println(key + "=" + configuration.get(key, "<absent>"));
        }
    }

    private static void validateTickle() {
        try {
            TickleValidation.validate();
            System.out.println("tickle=OK");
        } catch (RuntimeException e) {
            // A negative cooldown fails validation with a ConfigException naming the key, no
            // cause. A non-numeric or missing value fails the read itself with an
            // IllegalStateException that also names the key, keeping the NumberFormatException
            // (or similar) as its cause - printed here too, so the reason reaches this process's
            // stdout, not just the key.
            System.out.println("ERROR: " + e.getMessage());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                System.out.println("Caused by: " + cause.getMessage());
            }
            System.exit(1);
        }
    }

    private static void printNavigatorEntries() {
        Set<String> names = NavigatorValidation.resolvedEntryNames();
        System.out.println("navigator.entries=" + String.join(",", names));
    }
}
