/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.season;

import net.onelitefeather.titan.common.feature.FeatureAudience;
import net.onelitefeather.titan.common.feature.FeatureGate;
import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.user.NoOpUserProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Shared fixtures for the season tests: a real {@link FeatureGate} on a fixed clock, a writable
 * permission source, and the JSON the tests feed the loader.
 *
 * <p>The gate is the production one, not a stub. A season test that faked the gate would prove
 * nothing about US-4.07, since the whole requirement is that seasons go through the same gate
 * everything else does.
 */
final class SeasonFixtures {

    /** The zone every season in these tests is planned in. */
    static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    /** "Now" for every test: mid-October 2026, chosen so both open and shut windows are easy. */
    static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    private SeasonFixtures() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Creates a gate on a fixed clock, over the given permission source.
     *
     * @param audience the permission answers the gate reads
     * @return the gate
     */
    static FeatureGate gate(FeatureAudience audience) {
        FeatureManager manager = new FeatureManagerBuilder().featureEnum(net.onelitefeather.titan.common.feature.TitanFeatures.class).stateRepository(new InMemoryStateRepository()).userProvider(new NoOpUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
        return FeatureGate.with(manager, audience, Clock.fixed(NOW, ZoneOffset.UTC), BERLIN);
    }

    /**
     * Writes a season file into a directory.
     *
     * @param directory where to write
     * @param name      the file name, without the extension
     * @param json      the file content
     * @return the file that was written
     * @throws java.io.IOException when the file cannot be written
     */
    static Path write(Path directory, String name, String json) throws java.io.IOException {
        Path file = directory.resolve(name + SeasonLoader.FILE_EXTENSION);
        Files.writeString(file, json);
        return file;
    }

    /**
     * Builds a minimal season file with an explicit window and one navigator icon swap, so a test
     * can tell two seasons apart by what they show.
     *
     * @param id       the season id
     * @param priority the priority
     * @param from     the window start, or {@code null}
     * @param to       the window end, or {@code null}
     * @param material the material this season puts on the {@code Survival} icon
     * @return the season as JSON
     */
    static String seasonJson(String id, int priority, String from, String to, String material) {
        String window = from == null && to == null ? "{}" : "{" + (from == null ? "" : "\"from\": \"" + from + "\",") + (to == null ? "" : "\"to\": \"" + to + "\",") + "\"zone\": \"Europe/Berlin\"}";
        return """
                {
                  "id": "%s",
                  "priority": %d,
                  "stage": "ga",
                  "window": %s,
                  "effects": [
                    { "type": "replace_icon", "destination": "Survival", "material": "%s" }
                  ]
                }
                """.formatted(id, priority, window, material);
    }

    /**
     * A permission source a test can grant permissions in.
     */
    static final class MutableAudience implements FeatureAudience {

        private final Set<String> permissions = new HashSet<>();
        private final Set<String> groups = new HashSet<>();

        MutableAudience grant(UUID playerId, String permission) {
            this.permissions.add(playerId + "/" + permission);
            return this;
        }

        MutableAudience join(UUID playerId, String group) {
            this.groups.add(playerId + "/" + group);
            return this;
        }

        @Override
        public boolean hasPermission(UUID playerId, String permission) {
            return this.permissions.contains(playerId + "/" + permission);
        }

        @Override
        public boolean inGroup(UUID playerId, String group) {
            return this.groups.contains(playerId + "/" + group);
        }
    }
}
