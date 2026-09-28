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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The child process entry point {@link ConfigFileWatchIntegrationTest} launches: touches the
 * {@link Config} facade like {@code Titan} does, prints {@code READY}, then for every stdin line
 * prints {@code "VALUE " + Config.getLong(KEY, DEFAULT)}.
 *
 * <p>Every request re-reads the facade from scratch, so a later avaje-config file-watch reload
 * becomes visible on the next request without restarting. No {@code Config} mutator is ever called
 * from this class or the test that drives it.
 */
public final class ConfigReadChildMain {

    /** The key read on every request - an arbitrary, already-shipped module key. */
    static final String KEY = "tickle.cooldownMillis";

    /** The shipped classpath default for {@link #KEY} (see {@code application.yaml}). */
    static final long DEFAULT = 4000L;

    private ConfigReadChildMain() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("READY");
        System.out.flush();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("VALUE " + Config.getLong(KEY, DEFAULT));
                System.out.flush();
            }
        }
    }
}
