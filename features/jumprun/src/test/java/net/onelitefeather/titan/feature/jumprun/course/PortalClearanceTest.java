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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.PortalShape;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.FakeSpaceProbe;
import net.onelitefeather.titan.feature.jumprun.space.Heading;
import org.junit.jupiter.api.Test;

class PortalClearanceTest {

    private static final BlockPos START_BLOCK = new BlockPos(0, 10, 0);
    private static final Pos START_POINT = new Pos(0.5, 11.0, 0.5, 90f, 0f);
    private static final Heading EAST = new Heading(1.0, 0.0);

    private static CourseBlock block(int x, int y, int z) {
        return TestBlocks.at(new BlockPos(x, y, z), Surface.FULL);
    }

    private static boolean kept(List<PortalShape> shapes, CourseBlock from, CourseBlock to) {
        return PortalClearance.of(shapes).isKept(new Jump(from, to));
    }

    private static List<PortalShape> boxAt(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return List.of(new Box(new Vec(minX, minY, minZ), new Vec(maxX, maxY, maxZ)));
    }

    private static List<PortalShape> discAt(double x, double y, double z, double radius, Vec normal) {
        return List.of(new Disc(new Vec(x, y, z), radius, normal));
    }

    // --- box ----------------------------------------------------------------------------------------

    @Test
    void aTargetInsideABoxIsNotKept() {
        assertFalse(kept(boxAt(3, 8, -1, 4, 14, 1), block(0, 10, 0), block(3, 10, 0)), "inside the portal");
    }

    @Test
    void aTargetCloserThanThreeBlocksToABoxIsNotKept() {
        assertFalse(kept(boxAt(6, 8, 0, 6, 14, 0), block(0, 10, 0), block(3, 10, 0)), "two blocks of air between the cell and the box");
    }

    @Test
    void aTargetExactlyThreeBlocksFromABoxIsKept() {
        assertTrue(kept(boxAt(7, 8, 0, 7, 14, 0), block(0, 10, 0), block(3, 10, 0)), "three blocks of air are far enough");
    }

    @Test
    void aFlightPathThroughABoxIsNotKeptEvenWithTheTargetBeyond() {
        assertFalse(kept(boxAt(2, 8, 0, 2, 14, 0), block(0, 10, 0), block(8, 10, 0)), "the jump flies through the portal");
    }

    @Test
    void aBoxAboveTheJumpsHeightSpanDoesNotMatter() {
        assertTrue(kept(boxAt(3, 100, 0, 3, 110, 0), block(0, 10, 0), block(3, 10, 0)), "a portal far overhead is no obstacle");
    }

    @Test
    void aBoxDiagonallyCloseToTheTargetIsNotKept() {
        assertFalse(kept(boxAt(5, 8, 5, 5, 14, 5), block(0, 10, 0), block(2, 10, 2)), "the corner distance counts per axis");
    }

    // --- disc ---------------------------------------------------------------------------------------

    @Test
    void aTargetOnADiscIsNotKept() {
        assertFalse(kept(discAt(10.5, 11.0, 0.5, 2.0, new Vec(1, 0, 0)), block(7, 10, 0), block(10, 10, 0)), "in the ring");
    }

    @Test
    void aTargetCloserThanThreeBlocksToThePlaneOfADiscIsNotKept() {
        assertFalse(kept(discAt(10.5, 11.0, 0.5, 2.0, new Vec(1, 0, 0)), block(5, 10, 0), block(8, 10, 0)), "two blocks in front of the ring");
    }

    @Test
    void aTargetFarInFrontOfADiscIsKept() {
        assertTrue(kept(discAt(10.5, 11.0, 0.5, 2.0, new Vec(1, 0, 0)), block(0, 10, 0), block(3, 10, 0)), "seven blocks in front of the ring");
    }

    @Test
    void aTargetBesideTheRimOfADiscIsNotKept() {
        assertFalse(kept(discAt(10.5, 11.0, 0.5, 2.0, new Vec(1, 0, 0)), block(10, 10, 8), block(10, 10, 4)), "in the plane, two blocks from the rim");
    }

    @Test
    void aTargetFarBesideTheRimOfADiscIsKept() {
        assertTrue(kept(discAt(10.5, 11.0, 0.5, 2.0, new Vec(1, 0, 0)), block(10, 10, 12), block(10, 10, 8)), "in the plane, six blocks from the rim");
    }

    @Test
    void aFlightPathThroughADiscIsNotKept() {
        assertFalse(kept(discAt(5.5, 11.0, 0.5, 2.0, new Vec(1, 0, 0)), block(0, 10, 0), block(12, 10, 0)), "the jump flies through the ring");
    }

    @Test
    void aDiscAtTheApexOfTheJumpIsNotKeptThoughAStandingPlayerClearsIt() {
        assertFalse(kept(discAt(0.5, 17.5, 0.5, 1.0, new Vec(0, 1, 0)), block(0, 10, 0), block(3, 10, 0)), "the head reaches y=14 at the apex, two blocks short of the ring");
    }

    @Test
    void aDiscHighAboveTheApexOfTheJumpIsKept() {
        assertTrue(kept(discAt(0.5, 22.5, 0.5, 1.0, new Vec(0, 1, 0)), block(0, 10, 0), block(3, 10, 0)), "eight blocks above the apex is no obstacle");
    }

    // --- no portals ---------------------------------------------------------------------------------

    @Test
    void aLobbyWithoutPortalsKeepsEveryJump() {
        assertTrue(kept(List.of(), block(0, 10, 0), block(3, 10, 0)), "nothing to avoid");
    }

    // --- courses ------------------------------------------------------------------------------------

    private static final int LANDINGS = 60;

    @Test
    void aCourseKeepsThreeBlocksFromABoxInItsWay() {
        List<PortalShape> shapes = boxAt(8, 0, -3, 9, 200, 3);
        Course course = TestBlocks.course(START_POINT, START_BLOCK, EAST, TestBlocks.FAR_SPAWN, new FakeSpaceProbe(), RandomGeneratorFactory.of("L64X128MixRandom").create(5L), PortalClearance.of(shapes)).orElseThrow();

        for (int landing = 0; landing < LANDINGS; landing++) {
            course.window().forEach(shown -> assertTrue(distanceToBox(shown.pos(), 8, 9, -3, 3) >= 3, "block " + shown.pos() + " keeps three blocks from the box"));
            CourseBlock next = course.window().get(course.window().indexOf(course.current()) + 1);
            course.advanceTo(new Pos(next.pos().x() + 0.5, next.topY(), next.pos().z() + 0.5));
        }

        assertEquals(LANDINGS, course.currentIndex(), "the course went on for every landing");
    }

    /** Blocks of air between the cell and the box in X/Z, the larger of the two axes. */
    private static int distanceToBox(BlockPos pos, int minX, int maxX, int minZ, int maxZ) {
        int gapX = Math.max(0, Math.max(minX - pos.x() - 1, pos.x() - maxX - 1));
        int gapZ = Math.max(0, Math.max(minZ - pos.z() - 1, pos.z() - maxZ - 1));
        return Math.max(gapX, gapZ);
    }
}
