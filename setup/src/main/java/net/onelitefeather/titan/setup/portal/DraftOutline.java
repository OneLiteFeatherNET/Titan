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
package net.onelitefeather.titan.setup.portal;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Disc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The live preview points for a portal draft: corners, the box they span and the ring. Pure
 * geometry over plain values, so it does not depend on the draft type.
 */
public final class DraftOutline {

    /** Upper bound over the whole preview, markers and shape together. */
    public static final int MAX_POINTS = PortalOutline.MAX_POINTS;

    /** Ring radius shown while none has been chosen yet. */
    public static final double DEFAULT_RADIUS = 3;

    /** Radius suggestions offered by the guided flow. */
    public static final List<Double> RADIUS_SUGGESTIONS = List.of(2.0, 3.0, 5.0, 8.0);

    private DraftOutline() {
    }

    /**
     * Preview of a box draft. Set corners are marked at their block centres; with one corner the
     * box runs from it to {@code block}, the player's current block, with two it spans both.
     */
    public static @NotNull List<Vec> box(@NotNull List<? extends Point> corners, @NotNull Point block) {
        if (corners.isEmpty()) {
            return List.of();
        }
        List<Vec> points = new ArrayList<>();
        for (Point corner : corners) {
            points.add(new Vec(corner.blockX() + 0.5, corner.blockY() + 0.5, corner.blockZ() + 0.5));
        }
        Point first = corners.getFirst();
        Point second = corners.size() > 1 ? corners.get(1) : block;
        Vec lo = new Vec(Math.min(first.blockX(), second.blockX()), Math.min(first.blockY(), second.blockY()), Math.min(first.blockZ(), second.blockZ()));
        Vec hi = new Vec(Math.max(first.blockX(), second.blockX()) + 1, Math.max(first.blockY(), second.blockY()) + 1, Math.max(first.blockZ(), second.blockZ()) + 1);
        points.addAll(PortalOutline.boxEdges(lo, hi, MAX_POINTS - points.size()));
        return List.copyOf(points);
    }

    /**
     * Preview of a ring draft. Without a disc (radius not chosen yet) it is a circle of
     * {@link #DEFAULT_RADIUS} around the eye, facing along {@code look}.
     */
    public static @NotNull List<Vec> ring(@Nullable Disc disc, @NotNull Pos eye, @NotNull Vec look) {
        if (disc == null) {
            return PortalOutline.circle(eye.asVec(), DEFAULT_RADIUS, look, MAX_POINTS);
        }
        return PortalOutline.circle(disc.center(), disc.radius(), disc.normal(), MAX_POINTS);
    }
}
