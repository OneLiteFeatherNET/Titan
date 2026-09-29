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

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalValidatorTest {

    private static final Box VALID_BOX = new Box(new Vec(10, 64, 10), new Vec(14, 68, 11));
    private static final Disc VALID_DISC = new Disc(new Vec(0.5, 72, 40.5), 5.5, new Vec(0, 0, 1));

    private static Portal box(String id) {
        return new Portal(id, VALID_BOX, "Survival", null);
    }

    private static Portal withShape(PortalShape shape) {
        return new Portal("p", shape, "Survival", null);
    }

    private static List<PortalProblem> problemsOf(Portal... portals) {
        return PortalValidator.problems(List.of(portals));
    }

    @Test
    void reportsNothingForValidPortals() {
        List<PortalProblem> problems = problemsOf(box("survival"), new Portal("ring", VALID_DISC, "ElytraRace", "titan.portal"));

        assertTrue(problems.isEmpty(), "valid portals must yield no problems but got " + problems);
    }

    @Test
    void reportsNothingForNoPortals() {
        assertTrue(PortalValidator.problems(List.of()).isEmpty(), "an empty list is valid");
    }

    @Test
    void reportsARadiusOfZero() {
        List<PortalProblem> problems = problemsOf(withShape(new Disc(new Vec(0, 64, 0), 0, new Vec(0, 0, 1))));

        assertEquals(List.of(new PortalProblem("p", 0, "radius must be greater than 0 but was 0.0")), problems);
    }

    @Test
    void reportsANegativeRadius() {
        List<PortalProblem> problems = problemsOf(withShape(new Disc(new Vec(0, 64, 0), -2, new Vec(0, 0, 1))));

        assertEquals(List.of(new PortalProblem("p", 0, "radius must be greater than 0 but was -2.0")), problems);
    }

    @Test
    void reportsAZeroLengthNormal() {
        List<PortalProblem> problems = problemsOf(withShape(new Disc(new Vec(0, 64, 0), 3, Vec.ZERO)));

        assertEquals(List.of(new PortalProblem("p", 0, "normal must not have length 0")), problems);
    }

    @Test
    void reportsMinAboveMaxNamingTheAxis() {
        List<PortalProblem> problems = problemsOf(withShape(new Box(new Vec(0, 70, 0), new Vec(5, 64, 5))));

        assertEquals(List.of(new PortalProblem("p", 0, "min.y (70.0) is greater than max.y (64.0)")), problems);
    }

    @Test
    void reportsEveryAxisWhereMinIsAboveMax() {
        List<PortalProblem> problems = problemsOf(withShape(new Box(new Vec(9, 0, 9), new Vec(1, 5, 1))));

        assertEquals(List.of(
                new PortalProblem("p", 0, "min.x (9.0) is greater than max.x (1.0)"), new PortalProblem("p", 0, "min.z (9.0) is greater than max.z (1.0)")), problems);
    }

    @Test
    void acceptsMinEqualToMax() {
        List<PortalProblem> problems = problemsOf(withShape(new Box(new Vec(3, 64, 3), new Vec(3, 64, 3))));

        assertTrue(problems.isEmpty(), "a single-block box is valid but got " + problems);
    }

    @Test
    void reportsABlankId() {
        List<PortalProblem> problems = problemsOf(new Portal("  ", VALID_BOX, "Survival", null));

        assertEquals(List.of(new PortalProblem("  ", 0, "id must not be blank")), problems);
    }

    @Test
    void reportsAMissingId() {
        List<PortalProblem> problems = problemsOf(new Portal(null, VALID_BOX, "Survival", null));

        assertEquals(List.of(new PortalProblem(null, 0, "id must not be blank")), problems);
    }

    @Test
    void reportsABlankTask() {
        List<PortalProblem> problems = problemsOf(new Portal("p", VALID_BOX, "", null));

        assertEquals(List.of(new PortalProblem("p", 0, "task must not be blank")), problems);
    }

    @Test
    void reportsAMissingShape() {
        List<PortalProblem> problems = problemsOf(new Portal("p", null, "Survival", null));

        assertEquals(List.of(new PortalProblem("p", 0, "shape is missing")), problems);
    }

    @Test
    void reportsADuplicateIdOnTheSecondOccurrence() {
        List<PortalProblem> problems = problemsOf(box("same"), box("same"));

        assertEquals(List.of(new PortalProblem("same", 1, "duplicate id")), problems);
    }

    @Test
    void reportsSeveralProblemsAtOnce() {
        Portal broken = new Portal("", new Disc(new Vec(0, 64, 0), 0, Vec.ZERO), " ", null);

        List<PortalProblem> problems = problemsOf(broken);

        assertEquals(List.of(
                new PortalProblem("", 0, "id must not be blank"), new PortalProblem("", 0, "task must not be blank"), new PortalProblem("", 0, "radius must be greater than 0 but was 0.0"), new PortalProblem("", 0, "normal must not have length 0")), problems);
    }

    @Test
    void requireValidAcceptsValidPortals() {
        PortalValidator.requireValid("lobby", List.of(box("survival")));
    }

    @Test
    void requireValidNamesWorldIdAndReason() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PortalValidator.requireValid("worlds/winter", List.of(withShape(new Disc(new Vec(0, 64, 0), 0, new Vec(0, 0, 1))))));

        String message = failure.getMessage();
        assertTrue(message.contains("worlds/winter"), "message must name the world: " + message);
        assertTrue(message.contains("'p'"), "message must name the portal id: " + message);
        assertTrue(message.contains("radius must be greater than 0"), "message must name the reason: " + message);
    }

    @Test
    void requireValidListsEveryProblem() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PortalValidator.requireValid("lobby", List.of(box("a"), box("a"), new Portal("b", VALID_BOX, "", null))));

        String message = failure.getMessage();
        assertTrue(message.contains("duplicate id"), "message must name the duplicate: " + message);
        assertTrue(message.contains("task must not be blank"), "message must name the blank task: " + message);
    }

    @Test
    void requireValidIdentifiesAPortalWithoutIdByItsIndex() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PortalValidator.requireValid("lobby", List.of(box("a"), new Portal(null, VALID_BOX, "Survival", null))));

        String message = failure.getMessage();
        assertTrue(message.contains("portal #1: id must not be blank"), "message must identify the portal by its index: " + message);
        assertFalse(message.contains("'null'"), "message must not print a null id: " + message);
    }

    @Test
    void requireValidIdentifiesABlankIdPortalByItsIndex() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> PortalValidator.requireValid("lobby", List.of(new Portal(" ", VALID_BOX, "Survival", null))));

        assertTrue(failure.getMessage().contains("portal #0: id must not be blank"), "message must identify the portal by its index: " + failure.getMessage());
    }
}
