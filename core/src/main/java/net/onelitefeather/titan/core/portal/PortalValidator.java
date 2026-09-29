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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks portals in one place so that the lobby (failing at start) and the setup editor (reporting
 * in chat) reject the same data for the same reasons.
 */
public final class PortalValidator {

    private PortalValidator() {
    }

    /** Every problem of every portal; empty if all are usable. */
    public static List<PortalProblem> problems(List<Portal> portals) {
        List<PortalProblem> problems = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (int index = 0; index < portals.size(); index++) {
            Portal portal = portals.get(index);
            String id = portal.id();
            if (isBlank(id)) {
                problems.add(new PortalProblem(id, index, "id must not be blank"));
            } else if (!seenIds.add(id)) {
                problems.add(new PortalProblem(id, index, "duplicate id"));
            }
            if (isBlank(portal.task())) {
                problems.add(new PortalProblem(id, index, "task must not be blank"));
            }
            switch (portal.shape()) {
                case null -> problems.add(new PortalProblem(id, index, "shape is missing"));
                case Box box -> checkBox(id, index, box, problems);
                case Disc disc -> checkDisc(id, index, disc, problems);
            }
        }
        return problems;
    }

    /**
     * Fails with a message that names the world, each offending portal and its reason, so an
     * operator can fix the map file without a debugger.
     *
     * @throws IllegalStateException if any portal is unusable
     */
    public static void requireValid(String world, List<Portal> portals) {
        List<PortalProblem> problems = problems(portals);
        if (problems.isEmpty()) {
            return;
        }
        String details = problems.stream().map(problem -> problem.portalLabel() + ": " + problem.reason()).collect(Collectors.joining("; "));
        throw new IllegalStateException("Invalid portals in world '" + world + "': " + details);
    }

    private static void checkBox(String id, int index, Box box, List<PortalProblem> problems) {
        checkAxis(id, index, "x", box.min().x(), box.max().x(), problems);
        checkAxis(id, index, "y", box.min().y(), box.max().y(), problems);
        checkAxis(id, index, "z", box.min().z(), box.max().z(), problems);
    }

    private static void checkAxis(String id, int index, String axis, double min, double max, List<PortalProblem> problems) {
        if (min > max) {
            problems.add(new PortalProblem(id, index, "min." + axis + " (" + min + ") is greater than max." + axis + " (" + max + ")"));
        }
    }

    private static void checkDisc(String id, int index, Disc disc, List<PortalProblem> problems) {
        if (!(disc.radius() > 0)) {
            problems.add(new PortalProblem(id, index, "radius must be greater than 0 but was " + disc.radius()));
        }
        Vec normal = disc.normal();
        if (normal.length() == 0) {
            problems.add(new PortalProblem(id, index, "normal must not have length 0"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
