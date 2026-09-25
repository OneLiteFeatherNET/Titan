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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.onelitefeather.titan.common.config.ConfigurationFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration coverage for the {@code lobby-module-config} spec requirement "Overrides have a
 * fixed rank order": {@link ConfigurationFactory#initialise()} - reached through the static
 * {@code io.avaje.config.Config} facade {@link ConfigurationPrintMain} touches exactly the way
 * {@link net.onelitefeather.titan.app.Titan} does in production - resolves the shipped classpath
 * {@code application.yaml} (see {@code app/src/main/resources/application.yaml}), the working
 * directory's own {@code application.yaml}, its active profiles, an external file, environment
 * variables and system properties in the order the spec fixes. The classpath file's own
 * {@code spawn.simulationDistance} default is {@code 2} and its {@code tickle.cooldownMillis}
 * default is {@code 4000} throughout this test (see that file), so a case that does not override
 * either key resolves to those values, never {@code <absent>} - the lowest rank in "Overrides have
 * a fixed rank order" is the shipped default, not nothing.
 *
 * <p>{@code avaje-config} resolves files against the JVM's real working directory and reads
 * {@code System.getenv} directly, with no injectable provider (see {@code design.md}, decision 6's
 * spike result). Neither can be faked in-process without breaking Independent/Repeatable (F.I.R.S.T
 * - no {@code System.setProperty}/{@code getenv} tampering, no changing {@code user.dir}), so every
 * case here runs {@link ConfigurationPrintMain} in its own child JVM, started via {@link
 * ProcessBuilder} with a {@code @TempDir} as its working directory and a controlled environment:
 * {@code environment().clear()}, then only the variables the case needs (plus {@code PATH}/
 * {@code JAVA_HOME}, which a JVM needs to start cleanly on every platform). The child prints one
 * {@code key=value} line per requested configuration key to stdout; this test asserts on those
 * lines and, where the spec cares, on the temp directory's contents.
 */
class ConfigurationPrecedenceTest {

    private static final java.time.Duration TIMEOUT = java.time.Duration.ofSeconds(30);

    @DisplayName("A profile's application-<profile>.yaml overrides a value set in the base application.yaml")
    @Test
    void profileOverridesAValue(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "tickle:\n  cooldownMillis: 4000\n");
        Files.writeString(workingDir.resolve("application-dev.yaml"), "tickle:\n  cooldownMillis: 1000\n");

        Map<String, String> resolved = run(workingDir, Map.of("AVAJE_PROFILES", "dev"), List.of(), List.of("tickle.cooldownMillis"));

        Assertions.assertEquals("1000", resolved.get("tickle.cooldownMillis"), "the active profile's value must win over the base file");
    }

    @DisplayName("An active profile without a matching file falls back to the base application.yaml")
    @Test
    void profileWithoutFileFallsBackToBase(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 2\n");

        Map<String, String> resolved = run(workingDir, Map.of("AVAJE_PROFILES", "prod"), List.of(), List.of("spawn.simulationDistance"));

        Assertions.assertEquals("2", resolved.get("spawn.simulationDistance"), "with no application-prod.yaml, the base value must still apply");
    }

    @DisplayName("An environment variable overrides the value from application.yaml")
    @Test
    void environmentVariableBeatsFile(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 2\n");

        Map<String, String> resolved = run(workingDir, Map.of("SPAWN_SIMULATIONDISTANCE", "4"), List.of(), List.of("spawn.simulationDistance"));

        Assertions.assertEquals("4", resolved.get("spawn.simulationDistance"), "the environment variable must win over the file");
    }

    @DisplayName("A system property overrides an environment variable for the same key")
    @Test
    void systemPropertyBeatsEnvironmentVariable(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 2\n");

        Map<String, String> resolved = run(workingDir, Map.of("SPAWN_SIMULATIONDISTANCE", "4"), List.of("-Dspawn.simulationDistance=9"), List.of("spawn.simulationDistance"));

        Assertions.assertEquals("9", resolved.get("spawn.simulationDistance"), "the system property must win over both the file and the environment variable");
    }

    @DisplayName("An external file selected via CONFIG_FILE overrides the base application.yaml")
    @Test
    void externalFileViaConfigFileBeatsBase(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 2\n");
        Path externalFile = workingDir.resolve("external.yaml");
        Files.writeString(externalFile, "spawn:\n  simulationDistance: 3\n");

        Map<String, String> resolved = run(workingDir, Map.of("CONFIG_FILE", externalFile.toAbsolutePath().toString()), List.of(), List.of("spawn.simulationDistance"));

        Assertions.assertEquals("3", resolved.get("spawn.simulationDistance"), "the external file named by CONFIG_FILE must win over the base file");
    }

    @DisplayName("A first start without any configuration file resolves the shipped classpath default, and creates no file")
    @Test
    void firstStartWithoutAnyFileResolvesTheShippedDefault(@TempDir Path workingDir) throws IOException, InterruptedException {
        Map<String, String> resolved = run(workingDir, Map.of(), List.of(), List.of("spawn.simulationDistance", "tickle.cooldownMillis"));

        Assertions.assertEquals("2", resolved.get("spawn.simulationDistance"), "with no file in the working directory, the shipped classpath application.yaml's own default must apply");
        Assertions.assertEquals("4000", resolved.get("tickle.cooldownMillis"));
        try (var entries = Files.list(workingDir)) {
            Assertions.assertTrue(entries.findAny().isEmpty(), "reading configuration must never create a file in the working directory");
        }
    }

    @DisplayName("application.yaml in the working directory beats the shipped classpath default")
    @Test
    void fileInWorkingDirectoryBeatsTheShippedDefault(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 3\n");

        Map<String, String> resolved = run(workingDir, Map.of(), List.of(), List.of("spawn.simulationDistance"));

        Assertions.assertEquals("3", resolved.get("spawn.simulationDistance"), "the working directory's own application.yaml must win over the shipped classpath default (2)");
    }

    @DisplayName("An existing app.json is no longer read - it is ignored, and the shipped defaults apply")
    @Test
    void existingAppJsonIsIgnoredAndDefaultsApply(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("app.json"), "{\"tickleDuration\": 1000}");

        Map<String, String> resolved = run(workingDir, Map.of(), List.of(), List.of("tickle.cooldownMillis"));

        Assertions.assertEquals("4000", resolved.get("tickle.cooldownMillis"), "app.json is no longer converted or read at all; the shipped default must apply instead of any value app.json carries");
        Assertions.assertTrue(Files.exists(workingDir.resolve("app.json")), "app.json must be left exactly as found");
        Assertions.assertFalse(Files.exists(workingDir.resolve("application.yaml")), "reading configuration must never write application.yaml, migrated or otherwise");
    }

    @DisplayName("A syntactically broken application.yaml aborts the child cleanly, naming the file and the error position")
    @Test
    void brokenApplicationYamlAbortsCleanlyNamingFileAndPosition(@TempDir Path workingDir) throws IOException, InterruptedException {
        // Line 3 is missing the ":" after "maxHeight" - a mapping value where a key was expected,
        // exactly the class of syntax error the E2E smoke test found hangs the real process instead
        // of exiting (see lobby-module-config spec, scenario "Syntaktisch kaputte Datei").
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  minHeight: -64\n   maxHeight: 310\n");

        List<String> output = runExpectingFailure(workingDir, Map.of(), List.of());

        String joined = String.join("\n", output);
        Assertions.assertTrue(joined.contains("application.yaml"), "the failure must name application.yaml, output was:\n" + joined);
        Assertions.assertTrue(joined.contains("line 3"), "the failure must name the broken line, output was:\n" + joined);
    }

    /**
     * Starts {@link ConfigurationPrintMain} in a child JVM with {@code workingDir} as its working
     * directory, an environment containing only {@code env} (plus {@code PATH}/{@code JAVA_HOME}),
     * and {@code systemProperties} as additional {@code -D} flags, asking it to print {@code keys}.
     * Waits for the process with a timeout (never a sleep), asserts a clean exit, and parses every
     * {@code key=value} line the child printed - ignoring any other output (e.g. a stray log line)
     * by only ever looking for a line starting with one of the requested keys.
     */
    private static Map<String, String> run(Path workingDir, Map<String, String> env, List<String> systemProperties, List<String> keys) throws IOException, InterruptedException {
        ChildResult result = startAndWait(workingDir, env, systemProperties, keys);
        Assertions.assertTrue(result.finished(), "the child process must finish within " + TIMEOUT);
        Assertions.assertEquals(0, result.exitCode(), "the child process must exit cleanly; output was:\n" + String.join("\n", result.lines()));

        Map<String, String> resolved = new HashMap<>();
        for (String key : keys) {
            String prefix = key + "=";
            result.lines().stream().filter(line -> line.startsWith(prefix)).findFirst().ifPresent(line -> resolved.put(key, line.substring(prefix.length())));
        }
        return resolved;
    }

    /**
     * Like {@link #run}, but for a child expected to abort: waits for the process within {@link
     * #TIMEOUT} (never {@code 124}, the shell's own "killed by timeout" code, which would mean the
     * process hung instead of aborting), asserts the exit code is non-zero, and returns every line
     * the child printed - the caller inspects it for the broken file's name and error position.
     */
    private static List<String> runExpectingFailure(Path workingDir, Map<String, String> env, List<String> systemProperties) throws IOException, InterruptedException {
        ChildResult result = startAndWait(workingDir, env, systemProperties, List.of());
        Assertions.assertTrue(result.finished(), "the child process must finish within " + TIMEOUT + " instead of hanging");
        Assertions.assertNotEquals(0, result.exitCode(), "a broken application.yaml must abort the child with a non-zero exit code; output was:\n" + String.join("\n", result.lines()));
        return result.lines();
    }

    /**
     * The child process's outcome: whether it finished within {@link #TIMEOUT}, its exit code (only
     * meaningful if it finished), and every line it printed to stdout/stderr (merged, see {@link
     * ProcessBuilder#redirectErrorStream(boolean)}).
     */
    private record ChildResult(boolean finished, int exitCode, List<String> lines) {
    }

    private static ChildResult startAndWait(Path workingDir, Map<String, String> env, List<String> systemProperties, List<String> keys) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.addAll(systemProperties);
        command.add(ConfigurationPrintMain.class.getName());
        command.addAll(keys);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir.toFile());
        processBuilder.environment().clear();
        putIfPresent(processBuilder.environment(), "PATH");
        putIfPresent(processBuilder.environment(), "JAVA_HOME");
        processBuilder.environment().putAll(env);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        // Drained on a background thread, concurrently with waitFor: the child's output is small
        // (a handful of "key=value" lines) so an OS pipe-buffer deadlock is not a practical risk
        // here, but reading concurrently rather than only after waitFor returns means a genuinely
        // hung child is still caught by the timeout below, never by an unbounded blocking read.
        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException ignored) {
                // the child closed its stdout as part of exiting; nothing left to read
            }
        }, "configuration-precedence-test-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished = process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        outputReader.join(TimeUnit.SECONDS.toMillis(5));
        int exitCode = finished ? process.exitValue() : -1;
        return new ChildResult(finished, exitCode, List.copyOf(lines));
    }

    private static void putIfPresent(Map<String, String> target, String variable) {
        String value = System.getenv(variable);
        if (value != null) {
            target.put(variable, value);
        }
    }

    private static String javaExecutable() {
        return ProcessHandle.current().info().command().orElseGet(() -> Path.of(System.getProperty("java.home"), "bin", "java").toString());
    }
}
