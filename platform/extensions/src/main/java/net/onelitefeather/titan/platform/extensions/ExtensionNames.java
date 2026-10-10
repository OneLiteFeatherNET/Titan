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
package net.onelitefeather.titan.platform.extensions;

import java.util.Collection;
import java.util.List;

/** Extension names in a stable order, so the start log and the permission checks never flicker. */
final class ExtensionNames {

    private ExtensionNames() {
    }

    static List<String> sorted(Collection<String> names) {
        return names.stream().sorted().toList();
    }
}
