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
package net.onelitefeather.titan.app.bootstrap.reload;

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.app.module.RestartOutcome;

/**
 * The child process entry point {@code ConfigChangeFileWatchIntegrationTest} launches: touches the
 * static {@code io.avaje.config.Config} facade exactly like {@code Titan} does - built-in first,
 * no factory of its own in between - takes its current flat values as a {@link ConfigChangeHandler}
 * snapshot, wires that handler with a printing fake restart operation ({@link PrintingRestarter},
 * fixed module order {@code sit, tickle}) and a direct ({@link Runnable#run()}) tick executor,
 * registers it via {@code Config.onChange(...)}, prints
 * {@code READY}, then blocks reading stdin until the parent test closes it - so this JVM stays
 * alive long enough for avaje-config's own "ConfigTimer" daemon thread to notice a file change the
 * parent test makes after seeing {@code READY}.
 *
 * <p>No Minestom server is started here at all: {@link ConfigChangeHandler} needs only a restart
 * operation/module order pair and a tick {@link java.util.concurrent.Executor}, neither of which
 * this test cares comes from Minestom (see
 * {@code openspec/changes/config-reload-feature-flags/tasks.md},
 * task 5.1). No {@code Config} mutator is ever called from this class or the test that drives it -
 * {@link #revertWriter} would only be reached by an actually rejected module, which this test's
 * scenario never produces, and throws if it ever is called, per the "no {@code Config} mutator in
 * test code" rule (see {@code openspec/changes/avaje-config-facade/design.md}, decision 5).
 */
public final class ConfigWatchChildMain {

    private ConfigWatchChildMain() {
    }

    public static void main(String[] args) throws IOException {
        Configuration configuration = Config.asConfiguration();
        Map<String, String> snapshot = FlatConfigValues.of(configuration.asProperties());

        PrintingRestarter restarter = new PrintingRestarter(List.of("sit", "tickle"));
        ConfigRevertWriter revertWriter = (puts, removals) -> {
            throw new AssertionError("this scenario must never need a revert");
        };
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter::restart, restarter::moduleOrder, revertWriter, Runnable::run, snapshot);
        Config.onChange(handler);

        System.out.println("READY");
        System.out.flush();

        // Kept alive until the parent test closes this process's stdin - see the class javadoc.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                // Nothing to do with the line itself; its only purpose is to block until EOF.
            }
        }
    }

    /** Prints {@code "RESTARTED " + moduleId} on every {@link #restart(String)} call. */
    private static final class PrintingRestarter {

        private final List<String> order;

        PrintingRestarter(List<String> order) {
            this.order = order;
        }

        RestartOutcome restart(String moduleId) {
            System.out.println("RESTARTED " + moduleId);
            System.out.flush();
            return new RestartOutcome.Restarted();
        }

        List<String> moduleOrder() {
            return this.order;
        }
    }
}
