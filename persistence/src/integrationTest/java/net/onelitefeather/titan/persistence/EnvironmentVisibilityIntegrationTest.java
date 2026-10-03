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
package net.onelitefeather.titan.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.avaje.inject.BeanScope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves, in a child JVM with a real environment, that TITAN_DATABASE_URL reaches
 * {@code @RequiresProperty("titan.database.url")} although the key is in no file: with avaje-config
 * on the class path Avaje reads {@code Config}, which maps a key to its upper-case env spelling.
 */
class EnvironmentVisibilityIntegrationTest {

    @TempDir
    Path tempDir;

    private Path output;

    @BeforeEach
    void createOutputFile() {
        this.output = this.tempDir.resolve("probe.log");
    }

    /** Builds the scope the way production does: no explicit plugin, only service loading. */
    public static final class Probe {
        public static void main(String[] args) {
            try (BeanScope scope = BeanScope.builder().build()) {
                System.out.println("STARTED");
            } catch (RuntimeException e) {
                for (Throwable t = e; t != null; t = t.getCause()) {
                    System.out.println("CAUSE " + t.getClass().getName() + ": " + t.getMessage());
                }
            }
        }
    }

    private static String runProbe(Path output, String... environment) throws IOException, InterruptedException {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-cp", System.getProperty("java.class.path"), Probe.class.getName());
        builder.redirectErrorStream(true);
        builder.redirectOutput(output.toFile());
        for (int i = 0; i < environment.length; i += 2) {
            builder.environment().put(environment[i], environment[i + 1]);
        }
        Process process = builder.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("probe JVM did not finish within 60s, output so far: " + Files.readString(output));
        }
        return Files.readString(output);
    }

    @Test
    void environmentVariableSwitchesTheDatabaseOn() throws Exception {
        String output = runProbe(this.output, "TITAN_DATABASE_URL", "jdbc:postgresql://127.0.0.1:1/x", "TITAN_DATABASE_POOL_MAXIMUMSIZE", "1", "TITAN_DATABASE_POOL_CONNECTIONTIMEOUTMS", "250");

        assertTrue(output.contains("PoolInitializationException"), "TITAN_DATABASE_URL must make the factory try to connect, output: " + output);
    }

    @Test
    void unsetEnvironmentKeepsTheDatabaseOff() throws Exception {
        String output = runProbe(this.output);

        assertEquals("STARTED", output.strip().lines().reduce((first, last) -> last).orElse(""), "without TITAN_DATABASE_URL the scope must start without a database, output: " + output);
    }
}
