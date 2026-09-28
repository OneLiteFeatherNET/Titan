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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration coverage (child JVM, no Minestom server) for avaje-config's built-in file watcher
 * ({@code config.watch.enabled}) making a changed value visible through the static {@code Config}
 * facade on the next read - with no module restart and no handler reacting to the change - and for
 * {@code ConfigFeatureFlags} reading a flag from an environment variable rather than a
 * {@code flags.properties} file.
 *
 * <p>Each scenario runs in its own child JVM, started via {@link ProcessBuilder} with a
 * {@code @TempDir} as its working directory - {@code avaje-config} resolves files against the real
 * working directory and reads {@code System.getenv} directly, with no injectable provider, so
 * neither can be faked in-process without breaking Independent/Repeatable. No {@code Config}
 * mutator ({@code setProperty}/{@code putAll}/{@code clearProperty}/{@code eventBuilder}) is ever
 * called from this test or the child mains it drives.
 */
class ConfigFileWatchIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @DisplayName("A value changed in a watched file becomes visible through the facade on the next read, with no restart")
    @Test
    void changedValueBecomesVisibleThroughTheFacadeWithNoRestart(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("application.yaml"), """
                config.watch.enabled: true
                config.watch.delay: 1
                config.watch.period: 1
                tickle:
                  cooldownMillis: 4000
                """);

        try (ChildProcess child = ChildProcess.start(workingDir, Map.of(), ConfigReadChildMain.class)) {
            child.awaitLine("READY"::equals, TIMEOUT);

            Assertions.assertEquals("VALUE 4000", child.request(TIMEOUT), "the first read must resolve the file's own value");

            // Changing only the digit count (4000 -> 9999) also changes the file's length, not only
            // its last-modified time - avaje-config's FileWatch compares lastModified OR length, so
            // this guarantees the change is detected regardless of the filesystem's mtime
            // granularity.
            Files.writeString(workingDir.resolve("application.yaml"), """
                    config.watch.enabled: true
                    config.watch.delay: 1
                    config.watch.period: 1
                    tickle:
                      cooldownMillis: 9999
                    """);

            // A condition wait bounded by an overall deadline, never a fixed sleep: repeatedly asks
            // the still-running child for its current read of the facade until avaje-config's own
            // "ConfigTimer" daemon thread has noticed the change and applied it, or the deadline
            // passes.
            long deadlineNanos = System.nanoTime() + TIMEOUT.toNanos();
            String lastValue = null;
            while (!"VALUE 9999".equals(lastValue)) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    throw new AssertionError("timed out after " + TIMEOUT + " waiting for the reloaded value, last read was: " + lastValue + ", output so far:\n" + String.join("\n", child.linesSoFar()));
                }
                lastValue = child.request(Duration.ofNanos(remainingNanos));
            }
        }
    }

    @DisplayName("An environment variable turns a feature flag on")
    @Test
    void environmentVariableTurnsTheFlagOn(@TempDir Path workingDir) throws IOException, InterruptedException {
        List<String> output = runOneShot(workingDir, Map.of("FEATURES_NAVIGATOR_SLENDER", "true"), FeatureFlagChildMain.class);

        Assertions.assertTrue(output.contains("NAVIGATOR_SLENDER=true"), "the environment variable must turn the flag on, output was:\n" + String.join("\n", output));
    }

    @DisplayName("A leftover flags.properties in the working directory has no effect")
    @Test
    void leftoverFlagsPropertiesFileHasNoEffect(@TempDir Path workingDir) throws IOException, InterruptedException {
        Files.writeString(workingDir.resolve("flags.properties"), "NAVIGATOR_SLENDER=true\n");

        List<String> output = runOneShot(workingDir, Map.of(), FeatureFlagChildMain.class);

        Assertions.assertTrue(
                output.contains("NAVIGATOR_SLENDER=false"), "flags.properties must no longer be read at all, output was:\n" + String.join("\n", output));
    }

    /**
     * Starts {@code mainClass} in a child JVM with {@code workingDir} as its working directory and
     * an environment containing only {@code env} (plus {@code PATH}/{@code JAVA_HOME}), waits for
     * it to exit within {@link #TIMEOUT}, asserts a clean exit, and returns every line it printed.
     */
    private static List<String> runOneShot(Path workingDir, Map<String, String> env, Class<?> mainClass) throws IOException, InterruptedException {
        try (ChildProcess child = ChildProcess.start(workingDir, env, mainClass)) {
            boolean finished = child.waitForExit(TIMEOUT);
            List<String> lines = child.linesSoFar();
            Assertions.assertTrue(finished, "the child process must finish within " + TIMEOUT + ", output so far:\n" + String.join("\n", lines));
            Assertions.assertEquals(0, child.exitValue(), "the child process must exit cleanly; output was:\n" + String.join("\n", lines));
            return lines;
        }
    }

    /**
     * A child {@link Process} whose stdout/stderr (merged) is drained on a background thread into
     * both a {@link BlockingQueue} ({@link #awaitLine}/{@link #request}, condition wait with an
     * overall timeout, no fixed sleep) and a plain, ever-growing list ({@link #linesSoFar()}, for
     * failure messages).
     */
    private static final class ChildProcess implements AutoCloseable {

        private final Process process;
        private final OutputStream stdin;
        private final BlockingQueue<String> pending = new LinkedBlockingQueue<>();
        private final List<String> seen = Collections.synchronizedList(new ArrayList<>());
        private final Thread outputReader;

        private ChildProcess(Process process) {
            this.process = process;
            this.stdin = process.getOutputStream();
            this.outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.seen.add(line);
                        this.pending.add(line);
                    }
                } catch (IOException ignored) {
                    // the child closed its stdout as part of exiting; nothing left to read
                }
            }, "config-file-watch-integration-test-output-reader");
            this.outputReader.setDaemon(true);
            this.outputReader.start();
        }

        static ChildProcess start(Path workingDir, Map<String, String> env, Class<?> mainClass) throws IOException {
            List<String> command = new ArrayList<>();
            command.add(javaExecutable());
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(mainClass.getName());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDir.toFile());
            processBuilder.environment().clear();
            putIfPresent(processBuilder.environment(), "PATH");
            putIfPresent(processBuilder.environment(), "JAVA_HOME");
            processBuilder.environment().putAll(env);
            processBuilder.redirectErrorStream(true);
            return new ChildProcess(processBuilder.start());
        }

        /**
         * Sends one line to the child's stdin - {@link ConfigReadChildMain} prints a fresh read of
         * the facade in response - and returns that response line, waiting no longer than
         * {@code timeout}.
         */
        String request(Duration timeout) throws IOException, InterruptedException {
            this.stdin.write("poll\n".getBytes(StandardCharsets.UTF_8));
            this.stdin.flush();
            return awaitLine(line -> line.startsWith("VALUE "), timeout);
        }

        /**
         * Blocks until a line matching {@code predicate} arrives, or fails the test once
         * {@code timeout} has elapsed without one - a condition wait bounded by an overall
         * deadline, never a fixed wall-clock sleep.
         */
        String awaitLine(Predicate<String> predicate, Duration timeout) throws InterruptedException {
            long deadlineNanos = System.nanoTime() + timeout.toNanos();
            while (true) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    throw new AssertionError("timed out after " + timeout + " waiting for a matching line, output so far:\n" + String.join("\n", linesSoFar()));
                }
                String line = this.pending.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (line != null && predicate.test(line)) {
                    return line;
                }
            }
        }

        /** Waits for the process to exit within {@code timeout}, then joins the output reader. */
        boolean waitForExit(Duration timeout) throws InterruptedException {
            boolean finished = this.process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            this.outputReader.join(TimeUnit.SECONDS.toMillis(5));
            return finished;
        }

        int exitValue() {
            return this.process.exitValue();
        }

        List<String> linesSoFar() {
            return List.copyOf(this.seen);
        }

        /**
         * Closes the child's stdin - both child mains exit cleanly once they see EOF - then waits
         * briefly for a clean exit, force-destroying it otherwise.
         */
        @Override
        public void close() throws IOException, InterruptedException {
            this.stdin.close();
            if (!this.process.waitFor(5, TimeUnit.SECONDS)) {
                this.process.destroyForcibly();
            }
            this.outputReader.join(TimeUnit.SECONDS.toMillis(5));
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
}
