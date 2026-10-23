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
 * The dancer's outside view: an invisible marker entity that only the dancer can see, placed by
 * {@link CameraPlacement}, which the dancer's client is set to look through. {@link #close()} gives
 * the view back to the dancer and removes the entity; it is safe to call once in every stop path.
 */
final class DanceCamera {

    private final Player dancer;
    private final Entity marker;

    private DanceCamera(Player dancer, Entity marker) {
        this.dancer = dancer;
        this.marker = marker;
    }

    /**
     * Spawns the camera for {@code dancer} and switches his view to it.
     *
     * @throws RuntimeException if the switch fails; the marker is removed again first
     */
    static DanceCamera open(Player dancer, Instance instance, Pos dancerPosition) {
        Entity marker = new Entity(EntityType.MARKER);
        marker.setAutoViewable(false);
        marker.setNoGravity(true);
        try {
            marker.setInstance(instance, CameraPlacement.place(dancerPosition, dancer.getEyeHeight())).join();
            marker.addViewer(dancer);
            dancer.spectate(marker);
        } catch (RuntimeException e) {
            marker.remove();
            throw e;
        }
        return new DanceCamera(dancer, marker);
    }

    /** Gives the dancer his own view back, then removes the camera entity. */
    void close() {
        try {
            this.dancer.stopSpectating();
        } finally {
            this.marker.remove();
        }
    }

    /** @return the marker entity the dancer looks through */
    Entity marker() {
        return this.marker;
    }
}
