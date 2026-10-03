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
package net.onelitefeather.titan.feature.jumprun;

import io.avaje.config.Configuration;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;

/** Fixture factory for blocks and courses whose material does not matter to the test. */
final class TestBlocks {

    /** A spawn so far away that its distance never matters to a test. */
    static final SpawnZone FAR_SPAWN = new SpawnZone(-1000.0, 0.0);

    private static final String SHIPPED_DEFAULTS = "titan/defaults/jumprun.yaml";

    private static final Palettes SHIPPED = JumprunSettings.palettes(shippedConfiguration());

    private TestBlocks() {
    }

    /**
     * A configuration holding only the shipped defaults, independent of the global one and of
     * what other tests set there. Each call returns a fresh, mutable one.
     */
    static Configuration shippedConfiguration() {
        return Configuration.builder().load(SHIPPED_DEFAULTS).build();
    }

    /** The shipped palettes; {@link Palettes} is immutable, so tests may share it. */
    static Palettes shipped() {
        return SHIPPED;
    }

    /** The shipped defaults with the palette of {@code surface} replaced by {@code weights}. */
    static Configuration shippedWith(Surface surface, Map<String, String> weights) {
        Configuration config = shippedConfiguration();
        config.forPath(JumprunSettings.key(surface)).keys().forEach(block -> config.clearProperty(JumprunSettings.key(surface, block)));
        weights.forEach((block, weight) -> config.setProperty(JumprunSettings.key(surface, block), weight));
        return config;
    }

    /** A lobby whose height limits are the given ones. */
    static LobbyHeightBounds bounds(int min, int max) {
        return TestBounds.fixed(min, max);
    }

    /** The shipped lobby limits (-64 to 310). */
    static final LobbyHeightBounds BOUNDS = bounds(-64, 310);

    static final HeightBand BAND = new HeightBand(BOUNDS);

    static JumprunConfig shippedReader() {
        return new JumprunConfig(shippedConfiguration());
    }

    /** A block with the first material of its shape. */
    static CourseBlock at(BlockPos pos, Surface surface) {
        return new CourseBlock(pos, surface, shipped().of(surface).blocks().getFirst());
    }

    static CourseGenerator generator(SpaceProbe probe, SpawnZone spawn, RandomGenerator random) {
        return generator(probe, spawn, random, Steering.none());
    }

    static CourseGenerator generator(SpaceProbe probe, SpawnZone spawn, RandomGenerator random, Steering steering) {
        return generator(probe, BAND, spawn, random, steering);
    }

    static CourseGenerator generator(SpaceProbe probe, HeightBand band, SpawnZone spawn, RandomGenerator random, Steering steering) {
        return new CourseGenerator(probe, band, spawn, random, shipped(), PortalClearance.NONE, steering);
    }

    static Optional<Course> course(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, RandomGenerator random) {
        return course(startPoint, startBlock, heading, spawn, probe, BAND, random);
    }

    static Optional<Course> course(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, HeightBand band, RandomGenerator random) {
        return Course.start(startPoint, startBlock, heading, spawn, probe, band, random, shipped());
    }

    static Optional<Course> course(Pos startPoint, BlockPos startBlock, Heading heading, SpawnZone spawn, SpaceProbe probe, RandomGenerator random, PortalClearance portals) {
        return Course.start(startPoint, startBlock, heading, spawn, probe, BAND, random, shipped(), portals);
    }
}
