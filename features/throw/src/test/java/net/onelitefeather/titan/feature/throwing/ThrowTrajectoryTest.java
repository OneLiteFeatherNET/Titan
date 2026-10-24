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
package net.onelitefeather.titan.feature.throwing;

import java.util.List;
import java.util.Optional;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrowTrajectoryTest {

    private static final Point ORIGIN = new Vec(0, 0, 0);
    private static final Vec EAST_PER_SECOND = new Vec(20, 0, 0);

    @DisplayName("The same input always gives the same points")
    @Test
    void sameInputGivesSamePoints() {
        ThrowTrajectory.Trajectory first = ThrowTrajectory.compute(ORIGIN, EAST_PER_SECOND, solidFromX(50));
        ThrowTrajectory.Trajectory second = ThrowTrajectory.compute(ORIGIN, EAST_PER_SECOND, solidFromX(50));

        Assertions.assertEquals(first, second, "the preview must be a pure function of its input");
    }

    @DisplayName("The first two steps follow drag 0.98 and gravity 0.08 per tick")
    @Test
    void stepsFollowDragAndGravity() {
        List<Vec> points = ThrowTrajectory.compute(ORIGIN, EAST_PER_SECOND, solidFromX(50)).points();

        assertNear(new Vec(0.98, -0.08, 0), points.get(0), "after one tick the velocity is drag-scaled and lowered by gravity");
        assertNear(new Vec(1.9404, -0.2384, 0), points.get(1), "after two ticks the drag compounds and gravity adds up");
    }

    private static void assertNear(Vec expected, Point actual, String message) {
        Assertions.assertEquals(expected.x(), actual.x(), 1e-9, message + " (x)");
        Assertions.assertEquals(expected.y(), actual.y(), 1e-9, message + " (y)");
        Assertions.assertEquals(expected.z(), actual.z(), 1e-9, message + " (z)");
    }

    @DisplayName("A free flight shows at most forty points")
    @Test
    void atMostFortyPoints() {
        ThrowTrajectory.Trajectory trajectory = ThrowTrajectory.compute(ORIGIN, EAST_PER_SECOND, solidFromX(1_000));

        Assertions.assertEquals(ThrowTrajectory.MAX_POINTS, trajectory.points().size(), "the preview must send exactly the cap when nothing is hit");
        Assertions.assertEquals(40, ThrowTrajectory.MAX_POINTS, "the cap is forty points per update");
    }

    @DisplayName("The trajectory ends before a solid block")
    @Test
    void endsBeforeASolidBlock() {
        ThrowTrajectory.Trajectory trajectory = ThrowTrajectory.compute(ORIGIN, EAST_PER_SECOND, solidFromX(3));

        Assertions.assertTrue(trajectory.points().stream().allMatch(point -> point.x() < 3), "no point may lie inside the solid block, points were " + trajectory.points());
    }

    @DisplayName("The landing marker sits on the last free point before a solid block")
    @Test
    void landingMarkerIsTheLastFreePoint() {
        ThrowTrajectory.Trajectory trajectory = ThrowTrajectory.compute(ORIGIN, EAST_PER_SECOND, solidFromX(3));

        Optional<Vec> landing = trajectory.landing();
        Assertions.assertTrue(landing.isPresent(), "a hit must produce a landing marker");
        Assertions.assertEquals(trajectory.points().getLast(), landing.get(), "the marker must be the last point of the trajectory");
    }

    @DisplayName("A flight that hits nothing has no landing marker")
    @Test
    void noLandingMarkerWithoutAHit() {
        ThrowTrajectory.Trajectory trajectory = ThrowTrajectory.compute(ORIGIN, EAST_PER_SECOND, solidFromX(1_000));

        Assertions.assertTrue(trajectory.landing().isEmpty(), "without a solid block in reach there is no landing marker");
    }

    private static java.util.function.Predicate<Point> solidFromX(double x) {
        return point -> point.x() >= x;
    }
}
