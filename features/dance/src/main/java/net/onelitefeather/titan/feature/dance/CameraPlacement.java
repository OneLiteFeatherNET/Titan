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

/**
 * Where the dance camera stands: in front of the dancer, at his eye height, looking back at him.
 */
final class CameraPlacement {

    /** How far in front of the dancer the camera stands, in blocks. */
    static final double DISTANCE = 3.5;

    private CameraPlacement() {
    }

    /**
     * @param dancer    the dancer's feet position; its yaw is the direction he faces
     * @param eyeHeight the dancer's eye height, in blocks
     * @return the camera position with the view direction towards the dancer's eyes
     */
    static Pos place(Pos dancer, double eyeHeight) {
        double eyeY = dancer.y() + eyeHeight;
        double yaw = Math.toRadians(dancer.yaw());
        // The direction the dancer faces, in Minecraft's yaw convention.
        double facingX = -Math.sin(yaw);
        double facingZ = Math.cos(yaw);
        Pos camera = new Pos(dancer.x() + facingX * DISTANCE, eyeY, dancer.z() + facingZ * DISTANCE);
        return camera.withLookAt(new Vec(dancer.x(), eyeY, dancer.z()));
    }
}
