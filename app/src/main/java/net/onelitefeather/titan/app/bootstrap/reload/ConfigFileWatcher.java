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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polls a fixed list of configuration file paths for changes and triggers a callback (production:
 * {@link ConfigReloader#reload()}) once per {@link #check()} call in which at least one of them
 * changed.
 *
 * <p>Built-in first: {@code java.nio.file.WatchService} was considered and rejected - it watches
 * directories, not files, and is unreliable for Kubernetes ConfigMaps (symlink swap) and some
 * network/overlay filesystems, which end up needing a poll-based fallback anyway. Polling
 * existence/last-modified/size is the same technique avaje-config's own {@code FileWatch} uses. See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 2.
 *
 * <p>Stamping is injected as a {@link Function}{@code <Path, FileStamp>} so this class can be
 * tested
 * without a real filesystem: production stamps a path via {@code Files.exists}/
 * {@code Files.getLastModifiedTime}/{@code Files.size}, a test via a fake in-memory map.
 *
 * <p><b>Baseline.</b> The first {@link #check()} call only records every path's current stamp; it
 * never triggers the callback. Nothing has "changed" yet relative to any prior observation, and
 * triggering a reload for every file that merely exists when the lobby starts would be surprising -
 * the lobby's own startup already reads them once. A later {@link #check()} then triggers exactly
 * when a path's stamp differs from the previous {@link #check()}'s.
 */
public final class ConfigFileWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileWatcher.class);

    private static final List<String> EXTENSIONS = List.of("yaml", "yml", "properties");
    private static final String BASE_NAME = "application";

    private final List<Path> paths;
    private final Function<Path, FileStamp> stampFunction;
    private final Runnable onChange;

    private Map<Path, FileStamp> lastStamps;

    public ConfigFileWatcher(List<Path> paths, Function<Path, FileStamp> stampFunction, Runnable onChange) {
        this.paths = List.copyOf(paths);
        this.stampFunction = Objects.requireNonNull(stampFunction, "stampFunction");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /**
     * Builds the path list a production {@link ConfigFileWatcher} watches:
     * {@code application.yaml},
     * {@code application.yml} and {@code application.properties} in {@code workDir}, the same three
     * names per entry in {@code activeProfiles} (e.g. {@code application-dev.yaml}), and finally
     * {@code configFile} if one was given (the file named by
     * {@code CONFIG_FILE}/{@code config.file}).
     * None of these paths need to exist yet.
     *
     * @param workDir        the working directory the lobby resolves {@code application.*} against
     * @param activeProfiles the currently active configuration profiles, e.g.
     *                       {@code List.of("dev")}
     * @param configFile     the file named by {@code CONFIG_FILE}/{@code config.file}, or
     *                       {@code null} if none is set
     * @return the ordered, de-duplicated path list to watch
     */
    public static List<Path> pathsFor(Path workDir, List<String> activeProfiles, Path configFile) {
        Objects.requireNonNull(workDir, "workDir");
        Objects.requireNonNull(activeProfiles, "activeProfiles");

        List<Path> paths = new ArrayList<>();
        for (String extension : EXTENSIONS) {
            addIfAbsent(paths, workDir.resolve(BASE_NAME + "." + extension));
        }
        for (String profile : activeProfiles) {
            for (String extension : EXTENSIONS) {
                addIfAbsent(paths, workDir.resolve(BASE_NAME + "-" + profile + "." + extension));
            }
        }
        if (configFile != null) {
            addIfAbsent(paths, configFile);
        }
        return List.copyOf(paths);
    }

    /**
     * @param intervalSeconds the value of {@code titan.config.reload.intervalSeconds}
     * @return {@code true} if a recurring poll should be scheduled at all - {@code false} for
     *         {@code 0} (and any non-positive value), which turns polling off; the caller (wired in
     *         a
     *         later wave) still schedules the reload command regardless
     */
    public static boolean shouldSchedule(int intervalSeconds) {
        return intervalSeconds > 0;
    }

    /**
     * Compares every watched path's current {@link FileStamp} against the previous call's, and, if
     * at least one differs, logs one DEBUG line per changed path and calls the {@code onChange}
     * callback exactly once. The very first call only establishes the baseline - see the class
     * javadoc.
     */
    public void check() {
        Map<Path, FileStamp> current = new LinkedHashMap<>();
        for (Path path : paths) {
            current.put(path, stampFunction.apply(path));
        }

        if (lastStamps == null) {
            lastStamps = current;
            return;
        }

        boolean changed = false;
        for (Path path : paths) {
            if (!Objects.equals(lastStamps.get(path), current.get(path))) {
                changed = true;
                LOGGER.debug("Configuration file changed: {}", path);
            }
        }

        lastStamps = current;
        if (changed) {
            onChange.run();
        }
    }

    private static void addIfAbsent(List<Path> paths, Path path) {
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }
}
