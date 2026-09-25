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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration coverage for {@link ConfigReloadPrintMain} - a reload driven by the real
 * avaje-config pipeline (production {@link AvajeConfigSnapshotSource}/{@link AvajeLiveConfig}), in
 * its own child JVM per case, exactly like {@code ConfigurationPrecedenceTest} does for the
 * initial load. See {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 5.4.
 *
 * <p>Every case: writes the starting file(s)/environment for this test, starts
 * {@link ConfigReloadPrintMain} with a {@code @TempDir} as its working directory, which touches
 * the facade once (the "before" state), applies exactly one file mutation (or none), triggers one
 * reload and prints the outcome plus the requested keys' resolved values - this test asserts on
 * that output, since it cannot reach into the child's memory.
 */
class ConfigReloadPrecedenceTest {

    private static final java.time.Duration TIMEOUT = java.time.Duration.ofSeconds(30);
    private static final String NO_MUTATION = "--none";
    private static final String DELETE_FILE = "--delete--";

    @DisplayName("A new value written to the file before reloading applies")
    @Test
    void newValueApplies(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 2\n");

        ChildResult result = run(workingDir, Map.of(), "application.yaml", "spawn:\n  simulationDistance: 7\n", List.of("spawn.simulationDistance"));

        assertCleanExit(result);
        Assertions.assertEquals("Applied", result.values().get("RESULT"), "a genuinely changed value must be reported as Applied");
        Assertions.assertEquals("7", result.values().get("spawn.simulationDistance"), "the new value must apply");
    }

    @DisplayName("An environment variable override still wins after the file is changed and reloaded")
    @Test
    void environmentVariableStillWinsAfterReload(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 2\n");

        ChildResult result = run(
                workingDir, Map.of("SPAWN_SIMULATIONDISTANCE", "9"), "application.yaml", "spawn:\n  simulationDistance: 5\n", List.of("spawn.simulationDistance"));

        assertCleanExit(result);
        Assertions.assertEquals("9", result.values().get("spawn.simulationDistance"), "the environment variable must still win over the file's new value");
        Assertions.assertEquals(
                "Unchanged", result.values().get("RESULT"), "the resolved value never actually changed (9 before and after), so nothing must be reported as applied");
    }

    @DisplayName("A key removed from the file falls back to the shipped classpath default")
    @Test
    void deletedKeyFallsBackToClasspathDefault(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "tickle:\n  cooldownMillis: 1000\n");

        ChildResult result = run(workingDir, Map.of(), "application.yaml", "spawn:\n  simulationDistance: 2\n", List.of("tickle.cooldownMillis"));

        assertCleanExit(result);
        Assertions.assertEquals("Applied", result.values().get("RESULT"));
        Assertions.assertEquals("4000", result.values().get("tickle.cooldownMillis"), "with the override gone, the shipped classpath default (4000) must apply");
    }

    @DisplayName("An application-<profile>.yaml created after start, with that profile active, is read on reload")
    @Test
    void newProfileFileCreatedAfterStartWithActiveProfileIsRead(@TempDir Path workingDir) throws IOException, InterruptedException {
        ChildResult result = run(
                workingDir, Map.of("AVAJE_PROFILES", "dev"), "application-dev.yaml", "tickle:\n  cooldownMillis: 1234\n", List.of("tickle.cooldownMillis"));

        assertCleanExit(result);
        Assertions.assertEquals("Applied", result.values().get("RESULT"));
        Assertions.assertEquals(
                "1234", result.values().get("tickle.cooldownMillis"), "the profile file, created only after startup, must still be picked up by the reload");
    }

    @DisplayName("A syntactically broken application.yaml changes nothing and reports Failed with the file and position")
    @Test
    void brokenYamlChangesNothing(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), "spawn:\n  simulationDistance: 2\n");
        // Same class of syntax error as ConfigurationPrecedenceTest's own broken-file fixture:
        // line 3 is missing the ":" after "maxHeight".
        String broken = "spawn:\n  minHeight: -64\n   maxHeight: 310\n";

        ChildResult result = run(workingDir, Map.of(), "application.yaml", broken, List.of("spawn.simulationDistance"));

        assertCleanExit(result);
        String joined = String.join("\n", result.lines());
        Assertions.assertEquals("Failed", result.values().get("RESULT"));
        Assertions.assertEquals("application.yaml", result.values().get("FILE"), "must name the broken file");
        // The detail message itself spans several printed lines (SnakeYAML's own multi-line
        // position message), so the "line 3" substring is checked against the full output rather
        // than the single-line DETAIL=... entry ChildResult.values() parses.
        Assertions.assertTrue(joined.contains("line 3"), "must name the broken line, output was:\n" + joined);
        Assertions.assertEquals("2", result.values().get("spawn.simulationDistance"), "a broken reload must leave the previous value in place");
    }

    @DisplayName("FEATURES_NAVIGATOR_SLENDER=true turns the flag on, and a reload leaves it on")
    @Test
    void environmentFeatureFlagTurnsOn(@TempDir Path workingDir) throws IOException, InterruptedException {
        ChildResult result = run(workingDir, Map.of("FEATURES_NAVIGATOR_SLENDER", "true"), NO_MUTATION, null, List.of("features.NAVIGATOR_SLENDER"));

        assertCleanExit(result);
        Assertions.assertEquals("true", result.values().get("features.NAVIGATOR_SLENDER"), "the environment variable must turn the flag on");
    }

    @DisplayName("A leftover flags.properties has no effect - the flag keeps its shipped default")
    @Test
    void leftoverFlagsPropertiesHasNoEffect(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("flags.properties"), "NAVIGATOR_SLENDER=true\n");

        ChildResult result = run(workingDir, Map.of(), NO_MUTATION, null, List.of("features.NAVIGATOR_SLENDER"));

        assertCleanExit(result);
        Assertions.assertEquals(
                "false", result.values().get("features.NAVIGATOR_SLENDER"), "flags.properties is not one of the files avaje-config loads, so it must have no effect");
    }

    private static void assertCleanExit(ChildResult result) {
        Assertions.assertTrue(result.finished(), "the child process must finish within " + TIMEOUT);
        Assertions.assertEquals(0, result.exitCode(), "the child process must exit cleanly; output was:\n" + String.join("\n", result.lines()));
    }

    /**
     * Starts {@link ConfigReloadPrintMain} in a child JVM with {@code workingDir} as its working
     * directory and an environment containing only {@code env} (plus {@code PATH}/
     * {@code JAVA_HOME}). {@code mutationFile} is either {@value #NO_MUTATION} or a path (relative
     * to {@code workingDir}) the child writes {@code mutationContent} to (or deletes, if
     * {@code mutationContent} is {@code null}) before triggering its one reload; {@code keys} are
     * the configuration keys to print afterwards. Waits for the process with a timeout (never a
     * sleep) and parses every {@code key=value} line it printed, plus its {@code RESULT}/
     * {@code FILE}/{@code DETAIL} lines, into {@link ChildResult#values()}.
     */
    private static ChildResult run(Path workingDir, Map<String, String> env, String mutationFile, String mutationContent, List<String> keys) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(ConfigReloadPrintMain.class.getName());
        command.add(mutationFile);
        if (!NO_MUTATION.equals(mutationFile)) {
            command.add(mutationContent == null ? DELETE_FILE : mutationContent);
        }
        command.addAll(keys);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir.toFile());
        processBuilder.environment().clear();
        putIfPresent(processBuilder.environment(), "PATH");
        putIfPresent(processBuilder.environment(), "JAVA_HOME");
        processBuilder.environment().putAll(env);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
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
        }, "config-reload-precedence-test-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished = process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        outputReader.join(TimeUnit.SECONDS.toMillis(5));
        int exitCode = finished ? process.exitValue() : -1;

        Map<String, String> values = new HashMap<>();
        for (String line : lines) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                values.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return new ChildResult(finished, exitCode, List.copyOf(lines), Map.copyOf(values));
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

    /**
     * The child process's outcome: whether it finished within {@link #TIMEOUT}, its exit code
     * (only meaningful if it finished), every line it printed (merged stdout/stderr), and every
     * {@code key=value}-shaped line among them parsed into a map - a superset covering both the
     * requested configuration keys and this main's own {@code RESULT}/{@code FILE}/{@code DETAIL}
     * lines.
     */
    private record ChildResult(boolean finished, int exitCode, List<String> lines, Map<String, String> values) {
    }
}
