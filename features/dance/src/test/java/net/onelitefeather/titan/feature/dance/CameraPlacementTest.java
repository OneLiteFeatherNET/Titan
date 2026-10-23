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
package net.onelitefeather.titan.feature.dance;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CameraPlacementTest {

    private static final double EYE_HEIGHT = 1.62;
    private static final double DELTA = 1e-6;

    @DisplayName("The camera stands the set distance in front of the dancer, facing the way he faces")
    @Test
    void cameraStandsInFrontOfTheDancer() {
        Pos dancer = new Pos(0, 64, 0, 0, 0);

        Pos camera = CameraPlacement.place(dancer, EYE_HEIGHT);

        Assertions.assertEquals(CameraPlacement.DISTANCE, camera.z(), DELTA, "yaw 0 faces +z, so the camera stands at +z");
        Assertions.assertEquals(0, camera.x(), DELTA, "no sideways offset when the dancer faces straight ahead");
    }

    @DisplayName("The camera is at the dancer's eye height")
    @Test
    void cameraIsAtEyeHeight() {
        Pos camera = CameraPlacement.place(new Pos(0, 64, 0, 0, 0), EYE_HEIGHT);

        Assertions.assertEquals(64 + EYE_HEIGHT, camera.y(), DELTA, "the camera must be at eye height");
    }

    @DisplayName("The camera looks at the dancer's eyes")
    @Test
    void cameraLooksAtTheDancer() {
        Pos dancer = new Pos(3, 70, -2, 135, 0);
        Pos camera = CameraPlacement.place(dancer, EYE_HEIGHT);

        Vec towardsEyes = new Vec(dancer.x() - camera.x(), (dancer.y() + EYE_HEIGHT) - camera.y(), dancer.z() - camera.z()).normalize();
        Vec viewed = camera.direction().normalize();

        Assertions.assertEquals(1.0, viewed.dot(towardsEyes), 1e-4, "the camera's view must point at the dancer's eyes");
    }

    @DisplayName("The camera follows the dancer's facing, not a fixed side")
    @Test
    void cameraFollowsTheFacing() {
        Pos facingEast = new Pos(0, 64, 0, -90, 0);

        Pos camera = CameraPlacement.place(facingEast, EYE_HEIGHT);

        Assertions.assertEquals(CameraPlacement.DISTANCE, camera.x(), DELTA, "facing east (yaw -90), the camera stands to the east of him, in front of his face");
        Assertions.assertEquals(0, camera.z(), DELTA, "no change along z for an east-facing dancer");
    }
}
