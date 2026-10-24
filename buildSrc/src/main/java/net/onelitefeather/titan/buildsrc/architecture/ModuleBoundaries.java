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
package net.onelitefeather.titan.buildsrc.architecture;

import java.util.List;
import java.util.Optional;

/**
 * The module boundary table of openspec {@code maintainability-gates} (design D2): which Titan
 * project a module may declare as a dependency. A pure function over Gradle project paths, so it
 * is testable without a Gradle project. Apps are the composition root and may depend on anything.
 */
public final class ModuleBoundaries {

    private static final List<String> NOTHING = List.of();
    private static final List<String> CORE = List.of(":core");
    private static final List<String> CORE_AND_COMMON = List.of(":core", ":common");
    private static final List<String> COLUMN = List.of(":core", ":persistence");
    private static final List<String> PLATFORM = List.of(":core", ":common", ":platform:extensions");

    private ModuleBoundaries() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Returns a message naming both modules when {@code source} may not depend on {@code target},
     * or empty when the dependency is allowed.
     *
     * @throws IllegalArgumentException if {@code source} is not in the table, so a new module
     *     is classified on purpose rather than allowed by default
     */
    public static Optional<String> violation(String source, String target) {
        if (source.equals(target) || source.startsWith(":apps:")) {
            return Optional.empty();
        }
        List<String> allowed = allowedTargets(source);
        if (allowed.contains(target)) {
            return Optional.empty();
        }
        return Optional.of(source + " must not depend on " + target + "; it may depend only on " + allowed);
    }

    private static List<String> allowedTargets(String source) {
        if (source.startsWith(":features:")) {
            return COLUMN;
        }
        if (source.startsWith(":platform:")) {
            return PLATFORM;
        }
        return switch (source) {
            // persistence may use core's test fixtures for its architecture test; its main code must not import core,
            // which the module's MaintainabilityArchitectureTest checks.
            case ":core" -> NOTHING;
            case ":persistence" -> CORE;
            case ":common" -> CORE;
            case ":setup", ":bridge" -> CORE_AND_COMMON;
            case ":runtime" -> List.of(":core", ":common", ":persistence");
            default -> throw new IllegalArgumentException("Module " + source + " is not classified in ModuleBoundaries; add it to the table");
        };
    }
}
