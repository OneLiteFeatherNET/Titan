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
        for (Portal portal : portals) {
            String id = portal.id();
            if (isBlank(id)) {
                problems.add(new PortalProblem(id, "id must not be blank"));
            } else if (!seenIds.add(id)) {
                problems.add(new PortalProblem(id, "duplicate id"));
            }
            if (isBlank(portal.task())) {
                problems.add(new PortalProblem(id, "task must not be blank"));
            }
            switch (portal.shape()) {
                case null -> problems.add(new PortalProblem(id, "shape is missing"));
                case Box box -> checkBox(id, box, problems);
                case Disc disc -> checkDisc(id, disc, problems);
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
        String details = problems.stream().map(problem -> "portal '" + problem.portalId() + "': " + problem.reason()).collect(Collectors.joining("; "));
        throw new IllegalStateException("Invalid portals in world '" + world + "': " + details);
    }

    private static void checkBox(String id, Box box, List<PortalProblem> problems) {
        checkAxis(id, "x", box.min().x(), box.max().x(), problems);
        checkAxis(id, "y", box.min().y(), box.max().y(), problems);
        checkAxis(id, "z", box.min().z(), box.max().z(), problems);
    }

    private static void checkAxis(String id, String axis, double min, double max, List<PortalProblem> problems) {
        if (min > max) {
            problems.add(new PortalProblem(id, "min." + axis + " (" + min + ") is greater than max." + axis + " (" + max + ")"));
        }
    }

    private static void checkDisc(String id, Disc disc, List<PortalProblem> problems) {
        if (!(disc.radius() > 0)) {
            problems.add(new PortalProblem(id, "radius must be greater than 0 but was " + disc.radius()));
        }
        Vec normal = disc.normal();
        if (normal.length() == 0) {
            problems.add(new PortalProblem(id, "normal must not have length 0"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
