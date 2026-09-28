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
package net.onelitefeather.titan.runtime.variant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A pure comparison of a variant's expected columns against the ones that actually loaded. */
public final class VariantColumns {

    private VariantColumns() {
    }

    /**
     * @return every name in {@code expected} that is missing from {@code loaded}, in
     *         {@code expected}'s own order; empty when every expected column loaded
     */
    public static List<String> missingColumns(List<String> expected, Collection<String> loaded) {
        Set<String> loadedNames = new HashSet<>(loaded);
        List<String> missing = new ArrayList<>();
        for (String name : expected) {
            if (!loadedNames.contains(name)) {
                missing.add(name);
            }
        }
        return List.copyOf(missing);
    }
}
