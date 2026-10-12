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
package net.onelitefeather.titan.core.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalApproachTest {

    /** Covers x 10..13, y 64..67, z 20..23 in world coordinates. */
    private static final Box BOX = new Box(new Vec(10, 64, 20), new Vec(12, 66, 22));
    private static final Disc RING = new Disc(new Vec(0, 70, 100), 5.0, new Vec(0, 0, 1));

    private static Portal portal(PortalShape shape, PortalLabel label) {
        return new Portal("p", shape, "Task", null, label);
    }

    private static PortalLabel labelAt(Vec position) {
        return new PortalLabel(position, "text", null, null, Billboard.CENTER, 0f);
    }

    private static Vec centreOf(PortalShape shape) {
        return switch (shape) {
            case Box box ->
                new Vec((box.min().x() + box.max().x() + 1) / 2, (box.min().y() + box.max().y() + 1) / 2, (box.min().z() + box.max().z() + 1) / 2);
            case Disc disc -> disc.center();
        };
    }

    private static void assertLooksAt(Pos stand, Vec target) {
        Pos expected = stand.add(0, PortalApproach.EYE_HEIGHT, 0).withLookAt(target);
        assertEquals(expected.yaw(), stand.yaw(), 0.01f, "yaw must point at the portal");
        assertEquals(expected.pitch(), stand.pitch(), 0.01f, "pitch must point at the portal");
    }

    @DisplayName("A box is approached from the face the reference point is nearest to")
    @Test
    void boxIsApproachedFromTheFaceOfTheReference() {
        Portal box = portal(BOX, null);

        Pos south = PortalApproach.standingBefore(box, new Vec(11.5, 64, 60));
        Pos north = PortalApproach.standingBefore(box, new Vec(11.5, 64, -30));
        Pos east = PortalApproach.standingBefore(box, new Vec(80, 64, 21.5));
        Pos west = PortalApproach.standingBefore(box, new Vec(-80, 64, 21.5));

        assertTrue(south.z() > 23, "south of the box, was " + south);
        assertTrue(north.z() < 20, "north of the box, was " + north);
        assertTrue(east.x() > 13, "east of the box, was " + east);
        assertTrue(west.x() < 10, "west of the box, was " + west);
    }

    @DisplayName("A box stands the player on its floor, centred on the chosen face")
    @Test
    void boxStandsOnItsFloorCentredOnTheFace() {
        Pos stand = PortalApproach.standingBefore(portal(BOX, null), new Vec(11.5, 64, 60));

        assertEquals(64, stand.y(), 1e-9, "feet at the box's bottom");
        assertEquals(11.5, stand.x(), 1e-9, "centred on the face");
    }

    @DisplayName("The label position decides the side of a box, before the reference point")
    @Test
    void labelPositionWinsOverTheReference() {
        Portal box = portal(BOX, labelAt(new Vec(11.5, 66, 16)));

        Pos stand = PortalApproach.standingBefore(box, new Vec(11.5, 64, 60));

        assertTrue(stand.z() < 20, "the label is north of the box, so that is the front, was " + stand);
    }

    @DisplayName("A box without any direction is approached from the south")
    @Test
    void boxWithoutDirectionDefaultsToSouth() {
        Pos stand = PortalApproach.standingBefore(portal(BOX, null), new Vec(11.5, 64, 21.5));

        assertTrue(stand.z() > 23, "south, was " + stand);
    }

    @DisplayName("A disc is approached from the side of its normal the reference is on")
    @Test
    void discIsApproachedFromTheNearerSideOfItsNormal() {
        Pos front = PortalApproach.standingBefore(portal(RING, null), new Vec(0, 64, 150));
        Pos back = PortalApproach.standingBefore(portal(RING, null), new Vec(0, 64, 50));

        assertTrue(front.z() > 100, "in front along +z, was " + front);
        assertTrue(back.z() < 100, "behind along -z, was " + back);
    }

    @DisplayName("A disc stands the player on the ring's lowest point")
    @Test
    void discStandsAtTheBottomOfTheRing() {
        Pos stand = PortalApproach.standingBefore(portal(RING, null), new Vec(0, 64, 150));

        assertEquals(65, stand.y(), 1e-9);
    }

    @DisplayName("A flat disc is approached horizontally from outside its rim")
    @Test
    void flatDiscIsApproachedFromOutsideTheRim() {
        Disc flat = new Disc(new Vec(0, 70, 0), 5.0, new Vec(0, 1, 0));

        Pos stand = PortalApproach.standingBefore(portal(flat, null), new Vec(40, 70, 0));

        assertTrue(stand.x() > 5, "outside the rim towards the reference, was " + stand);
    }

    @DisplayName("The player never stands in the trigger of any shape, nor enters it with a step towards the portal")
    @Test
    void neverStandsInsideTheTrigger() {
        Vec[] references = {new Vec(11.5, 64, 60), new Vec(-80, 64, 21.5), new Vec(0, 64, 150), new Vec(0, 64, 50), new Vec(3, 80, 3)};
        PortalShape[] shapes = {BOX, RING, new Disc(new Vec(10, 70, -20), 4.0, new Vec(0.6, 0.8, 0)), new Disc(new Vec(0, 70, 0), 5.0, new Vec(0, 1, 0))};
        for (PortalShape shape : shapes) {
            for (Vec reference : references) {
                Pos stand = PortalApproach.standingBefore(portal(shape, null), reference);
                Vec feet = stand.asVec();
                Vec towards = feet.add(centreOf(shape).sub(feet).normalize().mul(0.5));
                assertFalse(shape.crossedBy(stand, stand), shape + " from " + reference + ": standing at " + stand);
                assertFalse(shape.crossedBy(stand, towards), shape + " from " + reference + ": half a block towards the centre from " + stand);
            }
        }
    }

    @DisplayName("The player looks at the portal's centre")
    @Test
    void looksAtThePortalCentre() {
        Pos onBox = PortalApproach.standingBefore(portal(BOX, null), new Vec(11.5, 64, 60));
        Pos onRing = PortalApproach.standingBefore(portal(RING, null), new Vec(0, 64, 150));

        assertLooksAt(onBox, centreOf(BOX));
        assertLooksAt(onRing, RING.center());
    }
}
