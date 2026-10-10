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
package net.onelitefeather.titan.core.testfixtures;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

/**
 * A shipped variant jar running as a child JVM, started in a working directory that holds only a
 * minimal world and an operator {@code application.yaml}.
 *
 * <p>The child is stopped by the AOT training hook ({@code titan.aot.trainSeconds}), so it exits
 * with 0 shortly after its start line, through the same shutdown path production uses. Nothing
 * here is a fake: the real {@code TitanApplication} reads the real files.
 */
public final class BootedVariant implements AutoCloseable {

    private static final String TITAN_JAR_PROPERTY = "titan.jar";
    private static final long POLL_MILLIS = 100;

    /**
     * Operator overrides covering a number, a list and a nested section of the shipped defaults.
     */
    private static final String APPLICATION_YAML = """
            spawn:
              simulationDistance: 3
            sit:
              allowedBlocks:
                - minecraft:oak_stairs
            jumprun:
              rainbow:
                rerollTicks: 12
            """;

    private static final String MAP_JSON = """
            {"name":"world","spawn":{"x":0.5,"y":64.0,"z":0.5,"yaw":0.0,"pitch":0.0},"builders":[]}
            """;

    private final Process process;
    private final BlockingQueue<String> pending = new LinkedBlockingQueue<>();
    private final List<String> seen = Collections.synchronizedList(new ArrayList<>());
    private final Thread outputReader;

    private BootedVariant(Process process) {
        this.process = process;
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
        }, "boot-smoke-output-reader");
        this.outputReader.setDaemon(true);
        this.outputReader.start();
    }

    /** The shipped jar under test, as passed by the build through {@code -Dtitan.jar}. */
    public static Path titanJar() {
        String jar = System.getProperty(TITAN_JAR_PROPERTY);
        Assertions.assertNotNull(jar, "the build must pass the shipped jar via -D" + TITAN_JAR_PROPERTY);
        return Path.of(jar);
    }

    /**
     * Writes the fixture into {@code workingDir} and starts {@code jar} on a free loopback port.
     */
    public static BootedVariant start(Path jar, Path workingDir) throws IOException {
        writeFixture(workingDir);
        ProcessBuilder processBuilder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Dservice.bind.host=127.0.0.1", "-Dservice.bind.port=0", "-Dtitan.aot.trainSeconds=1", "-jar", jar.toAbsolutePath().toString());
        processBuilder.directory(workingDir.toFile());
        processBuilder.environment().clear();
        putIfPresent(processBuilder.environment(), "PATH");
        putIfPresent(processBuilder.environment(), "JAVA_HOME");
        processBuilder.redirectErrorStream(true);
        return new BootedVariant(processBuilder.start());
    }

    private static void writeFixture(Path workingDir) throws IOException {
        Path world = Files.createDirectories(workingDir.resolve("worlds").resolve("world"));
        Files.writeString(world.resolve("map.json"), MAP_JSON);
        Files.writeString(workingDir.resolve("application.yaml"), APPLICATION_YAML);
    }

    /**
     * Blocks until a line matches {@code predicate}, failing early if the child exits without one,
     * and at the latest when {@code timeout} has passed. Polls in short steps, never a fixed sleep.
     */
    public String awaitLine(Predicate<String> predicate, Duration timeout) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (true) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new AssertionError("timed out after " + timeout + " waiting for a matching line, log:\n" + String.join("\n", linesSoFar()));
            }
            String line = this.pending.poll(Math.min(remainingNanos, TimeUnit.MILLISECONDS.toNanos(POLL_MILLIS)), TimeUnit.NANOSECONDS);
            if (line != null && predicate.test(line)) {
                return line;
            }
            if (line == null && !this.process.isAlive()) {
                this.outputReader.join(TimeUnit.SECONDS.toMillis(5));
                if (this.pending.isEmpty()) {
                    throw new AssertionError("the child exited with " + this.process.exitValue() + " before a matching line appeared, log:\n" + String.join("\n", linesSoFar()));
                }
            }
        }
    }

    /** Waits up to {@code timeout} for the child to exit, then joins the output reader. */
    public boolean waitForExit(Duration timeout) throws InterruptedException {
        boolean finished = this.process.waitFor(timeout.toNanos(), TimeUnit.NANOSECONDS);
        this.outputReader.join(TimeUnit.SECONDS.toMillis(5));
        return finished;
    }

    public int exitValue() {
        return this.process.exitValue();
    }

    public List<String> linesSoFar() {
        return List.copyOf(this.seen);
    }

    /** Destroys the child if it is still running, so no test leaves a lobby behind. */
    @Override
    public void close() throws InterruptedException {
        if (this.process.isAlive()) {
            this.process.destroyForcibly();
        }
        this.process.waitFor(5, TimeUnit.SECONDS);
        this.outputReader.join(TimeUnit.SECONDS.toMillis(5));
    }

    private static void putIfPresent(Map<String, String> target, String variable) {
        String value = System.getenv(variable);
        if (value != null) {
            target.put(variable, value);
        }
    }
}
