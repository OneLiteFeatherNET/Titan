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
package net.onelitefeather.titan.apps.cloudnet.bootstrap;

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import net.onelitefeather.titan.runtime.bootstrap.ConfigurationStartupLog;

/**
 * The child process entry point {@link ConfigurationPrecedenceTest} launches: touches the
 * {@link Config} facade like {@code Titan} does, then prints either resolved {@code key=value}
 * configuration lines or the active-profiles log line, so the parent test can assert on this
 * process's own output.
 *
 * <p>{@link ConfigurationPrecedenceTest} controls this process's working directory, environment
 * and system properties via {@link ProcessBuilder}, so what it prints is exactly what a real lobby
 * process would resolve under the same conditions. A broken {@code application.yaml} fails the
 * facade's static initializer; {@link #main(String[])} catches it and prints the full cause chain.
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
     * Prints {@code throwable} as "ERROR: ...", then one "Caused by: ..." line per cause in its
     * chain.
     */
    private static void printCauseChain(Throwable throwable) {
        String message = throwable.getMessage();
        System.out.println("ERROR: " + (message != null ? message : throwable.toString()));
        for (Throwable cause = throwable.getCause(); cause != null; cause = cause.getCause()) {
            System.out.println("Caused by: " + cause.getMessage());
        }
    }
}
