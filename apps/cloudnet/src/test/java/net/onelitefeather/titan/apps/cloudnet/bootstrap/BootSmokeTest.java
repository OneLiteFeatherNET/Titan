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

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import net.onelitefeather.titan.core.testfixtures.BootedVariant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Boots the shipped {@code cloudnet} jar with a real {@code application.yaml} and world, the way
 * production starts it, and checks it reports its start, logs no ERROR and exits with 0.
 *
 * <p>Runs in the {@code bootSmokeTest} task (tag {@code boot}), not in {@code test}.
 */
@Tag("boot")
class BootSmokeTest {

    private static final String VARIANT = "cloudnet";
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration EXIT_TIMEOUT = Duration.ofSeconds(30);
    private static final Pattern ERROR_LINE = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3} ERROR ");

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.MINUTES)
    @DisplayName("The shipped cloudnet jar starts with its configuration and world, then exits cleanly")
    void shippedCloudnetJarStartsAndExitsCleanly(@TempDir Path workingDir) throws Exception {
        try (BootedVariant variant = BootedVariant.start(BootedVariant.titanJar(), workingDir)) {
            variant.awaitLine(line -> line.contains("Variant " + VARIANT + " started with modules"), STARTUP_TIMEOUT);

            boolean exited = variant.waitForExit(EXIT_TIMEOUT);
            List<String> log = variant.linesSoFar();

            Assertions.assertTrue(exited, "the " + VARIANT + " lobby must exit after its graceful stop within " + EXIT_TIMEOUT + ", log:\n" + String.join("\n", log));
            Assertions.assertEquals(0, variant.exitValue(), "the " + VARIANT + " lobby must exit with code 0, log:\n" + String.join("\n", log));
            List<String> errors = log.stream().filter(line -> ERROR_LINE.matcher(line).find()).toList();
            Assertions.assertTrue(errors.isEmpty(), "the " + VARIANT + " lobby must log no ERROR line, found:\n" + String.join("\n", errors));
        }
    }
}
