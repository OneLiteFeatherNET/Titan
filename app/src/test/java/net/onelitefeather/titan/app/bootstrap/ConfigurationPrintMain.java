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

/**
 * The child process entry point {@link ConfigurationPrecedenceTest} launches: touches the same
 * static {@link Config} facade {@code PlatformBeans}/{@code Titan} use - built-in first, no factory
 * of its own wraps that touch (see {@code openspec/changes/avaje-config-facade/design.md}, decision
 * 1) - then either prints one {@code key=value} line per requested key to stdout - {@code <absent>}
 * if the key resolves to nothing at all - or, for {@value #LOG_ACTIVE_PROFILES}, runs
 * {@link ConfigurationStartupLog#activeProfiles()}. Either way, the parent test process - which
 * cannot reach into this JVM's memory - asserts on what this process printed.
 *
 * <p>Two modes, chosen by {@code args[0]}:
 * <ul>
 * <li>{@value #LOG_ACTIVE_PROFILES}: runs {@link ConfigurationStartupLog#activeProfiles()} - the
 * exact call {@code Titan}'s constructor makes as its own first touch of the facade - so
 * {@link net.onelitefeather.titan.app.bootstrap.ConfigurationPrecedenceTest} can assert on the
 * INFO line it logs under a chosen profile. The line reaches this process's stdout via the
 * {@code CONSOLE} appender {@code common/src/main/resources/logback.xml} wires to {@code root}, the
 * same file production runs with, so the parent test can read it back merged with this process's
 * regular output (see {@link ConfigurationPrecedenceTest#startAndWait}).</li>
 * <li>anything else: every argument is a configuration key to print, in order, via
 * {@code Configuration.get(key, "<absent>")} - the original, plain read mode. This mode alone
 * covers reading a nested key such as {@code navigator.entries.parkour.slot} - printing it
 * demonstrates that a working-directory entry merges alongside the shipped defaults, without a
 * validation bridge of its own (see {@code openspec/changes/avaje-config-facade/design.md},
 * decision 6, and {@link NavigatorEntryKeysTest} for the pure name-grouping this stops short
 * of).</li>
 * </ul>
 * A module's own read-and-validate path (e.g. {@code tickle.cooldownMillis} through
 * {@code Config.getAs(key, TickleSettings::cooldownMillis)}) is covered by that module's own unit
 * tests against a local {@link Configuration} instance instead of a child JVM here - see
 * {@code TickleSettingsTest} - because {@code Config.getAs} wraps and names the key by itself
 * (built-in first), with nothing left for a bridge like this to add.
 *
 * <p>A syntactically broken {@code application.yaml} fails the facade's own static initializer on
 * first touch with {@link ExceptionInInitializerError}, whichever mode above makes that first
 * touch. {@link #main(String[])} catches it at the top level and prints the full cause chain via
 * {@link #printCauseChain(Throwable)}, so the file name and the parser's line/column reach this
 * process's stdout reliably, rather than relying on the JVM's own uncaught-exception formatting.
 *
 * <p>{@link ConfigurationPrecedenceTest} controls this process's working directory, environment and
 * system properties via {@link ProcessBuilder} before launching it, so what this class prints is
 * exactly what a real lobby process would resolve, or reject, under the same conditions.
 */
public final class ConfigurationPrintMain {

    private static final String LOG_ACTIVE_PROFILES = "--log-active-profiles";

    private ConfigurationPrintMain() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (ExceptionInInitializerError error) {
            printCauseChain(error);
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        if (args.length == 1 && LOG_ACTIVE_PROFILES.equals(args[0])) {
            ConfigurationStartupLog.activeProfiles();
            return;
        }

        Configuration configuration = Config.asConfiguration();
        for (String key : args) {
            System.out.println(key + "=" + configuration.get(key, "<absent>"));
        }
    }

    /**
     * Prints {@code throwable} as {@code ERROR: <message or toString()>}, then one
     * {@code Caused by: <cause message>} line per exception in its cause chain - e.g., for a broken
     * {@code application.yaml}, {@code ExceptionInInitializerError} (whose own
     * {@link Throwable#getMessage()} is {@code null}, hence the {@link Throwable#toString()}
     * fallback), then the {@code IllegalStateException} naming {@code application.yaml}, then
     * SnakeYAML's own message naming the line and column.
     */
    private static void printCauseChain(Throwable throwable) {
        String message = throwable.getMessage();
        System.out.println("ERROR: " + (message != null ? message : throwable.toString()));
        for (Throwable cause = throwable.getCause(); cause != null; cause = cause.getCause()) {
            System.out.println("Caused by: " + cause.getMessage());
        }
    }
}
