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
package net.onelitefeather.titan.feature.season;

import io.avaje.config.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and validates {@code seasons.*} from a {@link Configuration}.
 *
 * <p>At startup a problem in an enabled season aborts with the qualified key and the reason. Live
 * the same problem only yields no configuration and one WARN per value: acting on a broken value
 * would restart the lobby into a startup that then aborts, and the supervisor would loop.
 */
final class SeasonConfigReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonConfigReader.class);

    /** The zone and every enabled, valid season; disabled seasons are left out unread. */
    record SeasonConfig(ZoneId zone, List<Season> seasons) {
    }

    private record Problem(String key, String reason) {

        String describe() {
            return this.key + ": " + this.reason;
        }
    }

    private final Configuration config;
    private final Path worldsDirectory;
    private final SeasonCalendar calendar;
    // Problems already warned about, so an unchanged broken value is not logged every minute.
    private final Set<String> warned = new HashSet<>();

    SeasonConfigReader(Configuration config, Path worldsDirectory, SeasonCalendar calendar) {
        this.config = Objects.requireNonNull(config, "config");
        this.worldsDirectory = Objects.requireNonNull(worldsDirectory, "worldsDirectory");
        this.calendar = Objects.requireNonNull(calendar, "calendar");
    }

    /**
     * @throws IllegalStateException naming the first invalid key and the reason
     */
    SeasonConfig readAtStartup() {
        List<Problem> problems = new ArrayList<>();
        SeasonConfig config = read(problems);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(problems.getFirst().describe());
        }
        for (SeasonCalendar.Overlap overlap : this.calendar.overlaps(config.seasons())) {
            LOGGER.warn("Seasons {} and {} overlap, {} wins", overlap.winner().id(), overlap.loser().id(), overlap.winner().id());
        }
        return config;
    }

    /**
     * @return the configuration, or empty while any enabled season or the zone is invalid
     */
    synchronized Optional<SeasonConfig> readLive() {
        List<Problem> problems = new ArrayList<>();
        SeasonConfig config = read(problems);
        Set<String> current = new HashSet<>();
        for (Problem problem : problems) {
            current.add(problem.describe());
            if (this.warned.add(problem.describe())) {
                LOGGER.warn("Invalid season configuration, no restart will be scheduled: {}", problem.describe());
            }
        }
        this.warned.retainAll(current);
        return problems.isEmpty() ? Optional.of(config) : Optional.empty();
    }

    private SeasonConfig read(List<Problem> problems) {
        ZoneId zone;
        try {
            zone = this.config.getAs(SeasonSettings.ZONE_KEY, ZoneId::of);
        } catch (RuntimeException e) {
            problems.add(new Problem(SeasonSettings.ZONE_KEY, "invalid zone '" + this.config.get(SeasonSettings.ZONE_KEY, "") + "' (" + rootMessage(e) + ")"));
            return new SeasonConfig(ZoneId.systemDefault(), List.of());
        }
        List<Season> seasons = new ArrayList<>();
        for (String id : seasonIds()) {
            if (SeasonSettings.RESERVED_ID.equals(id)) {
                problems.add(new Problem(SeasonSettings.ZONE_KEY, "'" + id + "' is reserved and cannot be a season id"));
            } else
                if (this.config.getBool(SeasonSettings.key(id, SeasonSettings.ENABLED_FIELD), true)) {
                    readSeason(id, problems).ifPresent(seasons::add);
                }
        }
        return new SeasonConfig(zone, List.copyOf(seasons));
    }

    private Set<String> seasonIds() {
        Set<String> ids = new TreeSet<>();
        for (String key : this.config.asProperties().stringPropertyNames()) {
            if (key.startsWith(SeasonSettings.PREFIX)) {
                String rest = key.substring(SeasonSettings.PREFIX.length());
                int dot = rest.indexOf('.');
                if (dot > 0) {
                    ids.add(rest.substring(0, dot));
                }
            }
        }
        return ids;
    }

    private Optional<Season> readSeason(String id, List<Problem> problems) {
        int before = problems.size();
        String world = value(id, SeasonSettings.WORLD_FIELD, problems);
        LocalDateTime from = date(id, SeasonSettings.FROM_FIELD, problems);
        LocalDateTime to = date(id, SeasonSettings.TO_FIELD, problems);
        if (world != null) {
            checkWorld(id, world, problems);
        }
        if (from != null && to != null && !from.isBefore(to)) {
            problems.add(new Problem(SeasonSettings.key(id, SeasonSettings.FROM_FIELD), "must be before " + SeasonSettings.key(id, SeasonSettings.TO_FIELD) + " (" + from + " is not before " + to + ")"));
        }
        return problems.size() == before ? Optional.of(new Season(id, world, from, to, true)) : Optional.empty();
    }

    private void checkWorld(String id, String world, List<Problem> problems) {
        String key = SeasonSettings.key(id, SeasonSettings.WORLD_FIELD);
        Path directory = this.worldsDirectory.resolve(world).normalize();
        if (!this.worldsDirectory.normalize().equals(directory.getParent())) {
            problems.add(new Problem(key, "'" + world + "' must be a directory name directly under " + SeasonSettings.WORLDS_DIRECTORY + "/"));
        } else if (!Files.isDirectory(directory)) {
            problems.add(new Problem(key, "world directory " + SeasonSettings.WORLDS_DIRECTORY + "/" + world + " does not exist"));
        } else if (!Files.isRegularFile(directory.resolve(SeasonSettings.MAP_FILE_NAME))) {
            problems.add(new Problem(key, SeasonSettings.WORLDS_DIRECTORY + "/" + world + " has no " + SeasonSettings.MAP_FILE_NAME));
        }
    }

    private LocalDateTime date(String id, String field, List<Problem> problems) {
        String raw = value(id, field, problems);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeException e) {
            problems.add(new Problem(SeasonSettings.key(id, field), "'" + raw + "' is not a local date-time like 2026-12-01T00:00:00"));
            return null;
        }
    }

    private String value(String id, String field, List<Problem> problems) {
        String key = SeasonSettings.key(id, field);
        Optional<String> value = this.config.getOptional(key).map(String::strip).filter(raw -> !raw.isEmpty());
        if (value.isEmpty()) {
            problems.add(new Problem(key, "is required" + quotingHint(field)));
        }
        return value.orElse(null);
    }

    // An unquoted YAML date-time is parsed to a non-text type that avaje-config drops, so it reads as missing.
    private static String quotingHint(String field) {
        boolean isDate = SeasonSettings.FROM_FIELD.equals(field) || SeasonSettings.TO_FIELD.equals(field);
        return isDate ? " (quote date-times: " + field + ": \"2026-12-01T00:00:00\")" : "";
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
