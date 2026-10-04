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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

/**
 * Without team heads the plain heads of the palette show, and the profile never changes the shape.
 */
class HeadFallbackTest {

    private static final CourseBlock SOURCE = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
    private static final Heading EAST = new Heading(1.0, 0.0);
    private static final HeadSkin TEAM = new HeadSkin(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "textures", "signature");

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    private static List<CourseBlock> heads(Palettes palettes) {
        CourseGenerator generator = TestBlocks.generator(palettes, new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(1L));
        List<CourseBlock> course = new ArrayList<>(List.of(SOURCE));
        Phase phase = new Phase.Scored(10, EAST);
        for (int i = 0; i < 300; i++) {
            Optional<CourseBlock> next = generator.next(course, phase);
            if (next.isEmpty()) {
                break;
            }
            course.add(next.get());
            phase = generator.after(course, phase);
        }
        return course.stream().filter(block -> block.surface() == Surface.HEAD).toList();
    }

    @Test
    void anEmptyListMakesPlainHeadsOfThePaletteWithoutProfile() {
        List<CourseBlock> heads = heads(TestBlocks.shipped());

        assertFalse(heads.isEmpty(), "the walk made heads");
        for (CourseBlock head : heads) {
            assertEquals(Optional.empty(), head.skin(), "no profile");
            assertTrue(TestBlocks.shipped().of(Surface.HEAD).blocks().stream().anyMatch(material -> material.id() == head.material().id()), head.material() + " is a material of the head palette");
        }
    }

    @Test
    void theProfileChangesNeitherTopNorFootprintNorCost() {
        CourseBlock plain = heads(TestBlocks.shipped()).getFirst();
        CourseBlock team = heads(TestBlocks.shipped().withHeads(List.of(TEAM))).getFirst();

        assertEquals(plain.topY() - plain.pos().y(), team.topY() - team.pos().y(), "top");
        assertEquals(plain.steps(), team.steps(), "footprint");
        assertEquals(plain.surface().typeCost(), team.surface().typeCost(), "cost");
    }
}
