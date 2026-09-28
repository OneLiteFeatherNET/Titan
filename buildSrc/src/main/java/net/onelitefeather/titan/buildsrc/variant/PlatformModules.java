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
package net.onelitefeather.titan.buildsrc.variant;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a permission platform module's name into the expected-module id
 * {@code titan.app-variant}'s {@code expectedModules} list carries into
 * {@code variant.properties}, e.g. {@code "luckperms"} becomes {@code "luckpermsPlatform"} - the
 * same {@code "<name><Kind>"} pattern a feature column's own name already follows for
 * {@code "<name>Column"}. A pure function over strings, so it is testable without a Gradle
 * project.
 */
public final class PlatformModules {

    private static final String SUFFIX = "Platform";

    private PlatformModules() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static List<String> expectedModuleIdsOf(String... platformNames) {
        List<String> ids = new ArrayList<>(platformNames.length);
        for (String name : platformNames) {
            ids.add(name + SUFFIX);
        }
        return List.copyOf(ids);
    }
}
