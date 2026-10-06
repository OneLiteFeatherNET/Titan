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
package net.onelitefeather.titan.feature.jumprun.course;

import io.avaje.config.Configuration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.Heading;
import net.onelitefeather.titan.feature.jumprun.space.SpaceProbe;
import net.onelitefeather.titan.feature.jumprun.space.SpawnZone;
import net.onelitefeather.titan.feature.jumprun.space.Steering;

/** Fixture factory for blocks and courses whose material does not matter to the test. */
public final class TestBlocks {

    /** A spawn so far away that its distance never matters to a test. */
    public static final SpawnZone FAR_SPAWN = new SpawnZone(-1000.0, 0.0);

    private static final String SHIPPED_DEFAULTS = "titan/defaults/jumprun.yaml";

    private static final Palettes SHIPPED = shippedPalettes();

    private TestBlocks() {
    }

    /** The shipped defaults read without validation: fixtures need the blocks, not the checks. */
    private static Palettes shippedPalettes() {
        Configuration config = Configuration.builder().load(SHIPPED_DEFAULTS).build();
        Map<Surface, Palette> byShape = new EnumMap<>(Surface.class);
        for (Surface surface : Surface.values()) {
            Configuration section = config.forPath("jumprun.palettes." + surface.configKey());
            List<Palette.Weighted> entries = new ArrayList<>();
            section.keys().stream().sorted().forEach(name -> {
                int weight = Integer.parseInt(section.get(name).trim());
                if (weight > 0) {
                    entries.add(new Palette.Weighted(surface.shape(Block.fromKey("minecraft:" + name)), weight));
                }
            });
            byShape.put(surface, Palette.of(entries));
        }
        return new Palettes(byShape);
    }

    /** The shipped palettes; {@link Palettes} is immutable, so tests may share it. */
    public static Palettes shipped() {
        return SHIPPED;
    }

    /** A lobby whose height limits are the given ones. */
    public static LobbyHeightBounds bounds(int min, int max) {
        return TestBounds.fixed(min, max);
    }

    /** The shipped lobby limits (-64 to 310). */
    public static final LobbyHeightBounds BOUNDS = bounds(-64, 310);

    static final HeightBand BAND = new HeightBand(BOUNDS);

    /** A block with the first material of its shape. */
    public static CourseBlock at(BlockPos pos, Surface surface) {
        return new CourseBlock(pos, surface, shipped().of(surface).blocks().getFirst());
    }

    public static CourseGenerator generator(SpaceProbe probe, SpawnZone spawn, RandomGenerator random) {
        return generator(probe, spawn, random, Steering.none());
    }

    static CourseGenerator generator(SpaceProbe probe, SpawnZone spawn, RandomGenerator random, Steering steering) {
        return generator(probe, BAND, spawn, random, steering);
    }

    /** A generator over the given palettes, e.g. the shipped ones with team heads. */
    static CourseGenerator generator(Palettes palettes, SpaceProbe probe, SpawnZone spawn, RandomGenerator random) {
        return new CourseGenerator(probe, BAND, spawn, random, palettes, PortalClearance.NONE, Steering.none());
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
