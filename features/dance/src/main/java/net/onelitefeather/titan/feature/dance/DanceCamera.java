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
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

/**
 * The dancer's outside view: an invisible camera entity that only the dancer can see, placed by
 * {@link CameraPlacement}, which the dancer's client is set to look through. {@link #close()} gives
 * the view back to the dancer and removes the entity; it is safe to call once in every stop path.
 */
final class DanceCamera {

    private final Player dancer;
    private final Entity camera;

    private DanceCamera(Player dancer, Entity camera) {
        this.dancer = dancer;
        this.camera = camera;
    }

    /**
     * Spawns the camera for {@code dancer} and switches his view to it.
     *
     * @throws RuntimeException if the switch fails; the camera entity is removed again first
     */
    static DanceCamera open(Player dancer, Instance instance, Pos dancerPosition) {
        // An item display without an item renders nothing and has no size, so the client uses the
        // position as given; vanilla servers never send the marker entity the camera used before.
        Entity camera = new Entity(EntityType.ITEM_DISPLAY);
        camera.setAutoViewable(false);
        camera.setNoGravity(true);
        try {
            camera.setInstance(instance, CameraPlacement.place(dancerPosition, dancer.getEyeHeight())).join();
            camera.addViewer(dancer);
            dancer.spectate(camera);
        } catch (RuntimeException e) {
            camera.remove();
            throw e;
        }
        return new DanceCamera(dancer, camera);
    }

    /** Gives the dancer his own view back, then removes the camera entity. */
    void close() {
        try {
            this.dancer.stopSpectating();
        } finally {
            this.camera.remove();
        }
    }
}
