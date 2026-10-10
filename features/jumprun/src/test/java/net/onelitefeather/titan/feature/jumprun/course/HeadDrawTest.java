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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.head.HeadSkin;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.FakeSpaceProbe;
import net.onelitefeather.titan.feature.jumprun.space.Heading;
import org.junit.jupiter.api.Test;

/**
 * Which team skin a head gets: random, never the same one twice in a row, and not a block state.
 */
class HeadDrawTest {

    private static final CourseBlock SOURCE = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
    private static final Heading EAST = new Heading(1.0, 0.0);
    private static final HeadSkin ALEX = new HeadSkin(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "textures-alex", "signature-alex");
    private static final HeadSkin BOB = new HeadSkin(UUID.fromString("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6"), "textures-bob", "signature-bob");

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    private static CourseGenerator generator(List<HeadSkin> heads, long seed) {
        return TestBlocks.generator(TestBlocks.shipped().withHeads(heads), new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(seed));
    }

    /**
     * The heads of one long walk that starts where heads are about to unlock, in the order they
     * came.
     */
    private static List<CourseBlock> headsOfAWalk(CourseGenerator generator) {
        List<CourseBlock> course = new ArrayList<>(List.of(SOURCE));
        Phase phase = new Phase.Scored(10, EAST, Mode.HARD);
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
    void withTwoProfilesBothAppear() {
        List<CourseBlock> heads = headsOfAWalk(generator(List.of(ALEX, BOB), 1L));

        Set<HeadSkin> seen = new HashSet<>();
        heads.forEach(head -> seen.add(head.skin().orElseThrow()));
        assertEquals(Set.of(ALEX, BOB), seen, "both profiles over " + heads.size() + " heads");
    }

    @Test
    void neverTheSameProfileTwiceInARowWithTwoOrMore() {
        int made = 0;
        for (long seed = 1; seed <= 5; seed++) {
            List<CourseBlock> heads = headsOfAWalk(generator(List.of(ALEX, BOB), seed));

            made += heads.size();
            for (int i = 1; i < heads.size(); i++) {
                assertNotEquals(heads.get(i - 1).skin(), heads.get(i).skin(), "seed " + seed + ": heads " + (i - 1) + " and " + i);
            }
        }
        assertTrue(made > 3, "the walks made " + made + " heads");
    }

    @Test
    void aSingleProfileRepeats() {
        List<CourseBlock> heads = headsOfAWalk(generator(List.of(ALEX), 1L));

        assertTrue(heads.size() > 3, "the walk made heads");
        assertTrue(heads.stream().allMatch(head -> head.skin().equals(Optional.of(ALEX))), "every head shows the only profile");
    }

    @Test
    void theProfileIsNotPartOfTheBlockState() {
        List<CourseBlock> heads = headsOfAWalk(generator(List.of(ALEX, BOB), 2L));

        for (CourseBlock head : heads) {
            assertEquals(Block.PLAYER_HEAD.id(), head.material().id(), "a team head is a player head block");
            assertEquals(Set.of("rotation", "powered"), head.material().properties().keySet(), "the state has the turn, no profile");
        }
    }

    @Test
    void aRedrawnHeadShowsAnotherProfileAtTheSamePlace() {
        CourseGenerator generator = generator(List.of(ALEX, BOB), 3L);
        List<CourseBlock> heads = headsOfAWalk(generator);
        assertFalse(heads.isEmpty(), "the walk made heads");

        for (CourseBlock head : heads) {
            CourseBlock redrawn = generator.redrawn(head, Optional.empty(), Optional.empty());

            assertNotEquals(head.skin(), redrawn.skin(), "another profile");
            assertEquals(head.pos(), redrawn.pos(), "same place");
            assertEquals(head.material(), redrawn.material(), "same block, same turn");
        }
    }

    @Test
    void aRedrawnHeadNeverShowsTheProfileOfTheHeadBeforeOrAfterIt() {
        HeadSkin carol = new HeadSkin(UUID.fromString("00000000-0000-0000-0000-0000000000c1"), "textures-carol", "signature-carol");
        for (long seed = 1; seed <= 20; seed++) {
            CourseGenerator generator = generator(List.of(ALEX, BOB, carol), seed);
            CourseBlock middle = headsOfAWalk(generator(List.of(ALEX), 1L)).getFirst();
            CourseBlock bobHead = new CourseBlock(middle.pos(), middle.surface(), middle.material(), Optional.of(BOB));

            CourseBlock redrawn = generator.redrawn(bobHead, Optional.of(ALEX), Optional.of(carol));

            assertEquals(Optional.of(BOB), redrawn.skin(), "seed " + seed + ": only the own profile is left, which is no neighbour");
        }
    }

    @Test
    void aRedrawnHeadWithAFreeProfileLeavesItsOwnAndBothNeighbours() {
        HeadSkin carol = new HeadSkin(UUID.fromString("00000000-0000-0000-0000-0000000000c1"), "textures-carol", "signature-carol");
        HeadSkin dave = new HeadSkin(UUID.fromString("00000000-0000-0000-0000-0000000000d1"), "textures-dave", "signature-dave");
        CourseBlock sample = headsOfAWalk(generator(List.of(ALEX), 1L)).getFirst();
        CourseBlock bobHead = new CourseBlock(sample.pos(), sample.surface(), sample.material(), Optional.of(BOB));
        for (long seed = 1; seed <= 20; seed++) {
            CourseGenerator generator = generator(List.of(ALEX, BOB, carol, dave), seed);

            CourseBlock redrawn = generator.redrawn(bobHead, Optional.of(ALEX), Optional.of(carol));

            assertEquals(Optional.of(dave), redrawn.skin(), "seed " + seed + ": the only profile that is neither neighbour nor itself");
        }
    }

    @Test
    void theSameSeedDrawsTheSameProfiles() {
        assertEquals(headsOfAWalk(generator(List.of(ALEX, BOB), 7L)), headsOfAWalk(generator(List.of(ALEX, BOB), 7L)), "same seed, same heads");
    }
}
