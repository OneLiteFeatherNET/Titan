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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link ConfigFileWatcher}, driven entirely through its injected stamp function
 * -
 * no real filesystem (see {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 3.3).
 */
class ConfigFileWatcherTest {

    private static final Path APPLICATION_YAML = Path.of("application.yaml");

    @DisplayName("pathsFor lists application.{yaml,yml,properties} in the working directory")
    @Test
    void pathsForListsBaseNamesInTheWorkingDirectory() {
        Path workDir = Path.of("/srv/lobby");

        List<Path> paths = ConfigFileWatcher.pathsFor(workDir, List.of(), null);

        Assertions.assertEquals(
                List.of(
                        workDir.resolve("application.yaml"), workDir.resolve("application.yml"), workDir.resolve("application.properties")), paths);
    }

    @DisplayName("pathsFor adds the same three names per active profile, and the CONFIG_FILE path last")
    @Test
    void pathsForAddsProfilesAndConfigFile() {
        Path workDir = Path.of("/srv/lobby");
        Path configFile = Path.of("/etc/titan/override.yaml");

        List<Path> paths = ConfigFileWatcher.pathsFor(workDir, List.of("dev"), configFile);

        Assertions.assertEquals(
                List.of(
                        workDir.resolve("application.yaml"), workDir.resolve("application.yml"), workDir.resolve("application.properties"), workDir.resolve("application-dev.yaml"), workDir.resolve("application-dev.yml"), workDir.resolve("application-dev.properties"), configFile), paths);
    }

    @DisplayName("pathsFor omits the CONFIG_FILE path when none is given")
    @Test
    void pathsForOmitsAbsentConfigFile() {
        List<Path> paths = ConfigFileWatcher.pathsFor(Path.of(""), List.of(), null);

        Assertions.assertEquals(3, paths.size(), "no CONFIG_FILE path must be added when null was given");
    }

    @DisplayName("The first check establishes a baseline without triggering")
    @Test
    void firstCheckEstablishesBaselineWithoutTriggering() {
        FakeFilesystem filesystem = new FakeFilesystem();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 100L, 10L));
        AtomicInteger triggers = new AtomicInteger();
        ConfigFileWatcher watcher = new ConfigFileWatcher(List.of(APPLICATION_YAML), filesystem, triggers::incrementAndGet);

        watcher.check();

        Assertions.assertEquals(0, triggers.get(), "the very first check must only record the baseline");
    }

    @DisplayName("An unchanged file does not trigger on a later check")
    @Test
    void unchangedFileDoesNotTrigger() {
        FakeFilesystem filesystem = new FakeFilesystem();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 100L, 10L));
        AtomicInteger triggers = new AtomicInteger();
        ConfigFileWatcher watcher = new ConfigFileWatcher(List.of(APPLICATION_YAML), filesystem, triggers::incrementAndGet);

        watcher.check();
        watcher.check();

        Assertions.assertEquals(0, triggers.get(), "an unchanged stamp must never trigger");
    }

    @DisplayName("A file that is created after the baseline triggers")
    @Test
    void fileCreatedAfterBaselineTriggers() {
        FakeFilesystem filesystem = new FakeFilesystem();
        AtomicInteger triggers = new AtomicInteger();
        ConfigFileWatcher watcher = new ConfigFileWatcher(List.of(APPLICATION_YAML), filesystem, triggers::incrementAndGet);

        watcher.check();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 100L, 10L));
        watcher.check();

        Assertions.assertEquals(1, triggers.get(), "a newly created file must trigger exactly once");
    }

    @DisplayName("A file whose content changes (last-modified or size) triggers")
    @Test
    void changedFileTriggers() {
        FakeFilesystem filesystem = new FakeFilesystem();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 100L, 10L));
        AtomicInteger triggers = new AtomicInteger();
        ConfigFileWatcher watcher = new ConfigFileWatcher(List.of(APPLICATION_YAML), filesystem, triggers::incrementAndGet);

        watcher.check();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 200L, 12L));
        watcher.check();

        Assertions.assertEquals(1, triggers.get(), "a changed stamp must trigger");
    }

    @DisplayName("A file that is deleted after the baseline triggers")
    @Test
    void deletedFileTriggers() {
        FakeFilesystem filesystem = new FakeFilesystem();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 100L, 10L));
        AtomicInteger triggers = new AtomicInteger();
        ConfigFileWatcher watcher = new ConfigFileWatcher(List.of(APPLICATION_YAML), filesystem, triggers::incrementAndGet);

        watcher.check();
        filesystem.remove(APPLICATION_YAML);
        watcher.check();

        Assertions.assertEquals(1, triggers.get(), "a deleted file must trigger exactly once");
    }

    @DisplayName("Several files changing in the same check trigger the callback exactly once")
    @Test
    void severalChangesTriggerOnlyOnce() {
        Path applicationYml = Path.of("application.yml");
        FakeFilesystem filesystem = new FakeFilesystem();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 100L, 10L));
        filesystem.set(applicationYml, new FileStamp(true, 100L, 10L));
        AtomicInteger triggers = new AtomicInteger();
        ConfigFileWatcher watcher = new ConfigFileWatcher(List.of(APPLICATION_YAML, applicationYml), filesystem, triggers::incrementAndGet);

        watcher.check();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 200L, 10L));
        filesystem.set(applicationYml, new FileStamp(true, 200L, 10L));
        watcher.check();

        Assertions.assertEquals(1, triggers.get(), "one check with several changed files must still trigger only once");
    }

    @DisplayName("A changed file logs a DEBUG line naming it")
    @Test
    void changedFileLogsDebugLine() {
        FakeFilesystem filesystem = new FakeFilesystem();
        filesystem.set(APPLICATION_YAML, new FileStamp(true, 100L, 10L));
        ConfigFileWatcher watcher = new ConfigFileWatcher(List.of(APPLICATION_YAML), filesystem, () -> {
        });

        Logger logger = (Logger) LoggerFactory.getLogger(ConfigFileWatcher.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            watcher.check();
            filesystem.set(APPLICATION_YAML, new FileStamp(true, 200L, 10L));
            watcher.check();
        } finally {
            logger.setLevel(originalLevel);
            logger.detachAppender(appender);
        }

        boolean found = appender.list.stream().anyMatch(event -> event.getLevel() == Level.DEBUG && "Configuration file changed: {}".equals(event.getMessage()));
        Assertions.assertTrue(found, "a changed file must log a DEBUG line with the file path as its argument");
    }

    @DisplayName("shouldSchedule is off for 0 and on for a positive interval")
    @Test
    void shouldScheduleReflectsTheInterval() {
        Assertions.assertFalse(ConfigFileWatcher.shouldSchedule(0), "0 must turn polling off");
        Assertions.assertFalse(ConfigFileWatcher.shouldSchedule(-1), "a negative interval must not schedule anything either");
        Assertions.assertTrue(ConfigFileWatcher.shouldSchedule(10), "a positive interval must schedule polling");
    }

    /**
     * An in-memory {@code Path -> FileStamp} stand-in for the filesystem, mutable between checks.
     */
    private static final class FakeFilesystem implements java.util.function.Function<Path, FileStamp> {

        private final Map<Path, FileStamp> stamps = new HashMap<>();

        void set(Path path, FileStamp stamp) {
            stamps.put(path, stamp);
        }

        void remove(Path path) {
            stamps.remove(path);
        }

        @Override
        public FileStamp apply(Path path) {
            return stamps.getOrDefault(path, FileStamp.MISSING);
        }
    }
}
