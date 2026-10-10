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
package net.onelitefeather.titan.common.deliver;

import java.util.List;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * Cross-classloader bridge for player counts of CloudNet services. The CloudNet service provider
 * runs in the bridge extension's own classloader, unreachable from the application, so this
 * holder lives on the shared application classloader and exchanges only JDK types; until the
 * bridge installs a {@link PlayerCountLookup}, nothing runs.
 */
public final class TitanPlayerCountLookup {

    private static volatile PlayerCountLookup lookup;

    private TitanPlayerCountLookup() {
    }

    public static void setLookup(PlayerCountLookup playerCountLookup) {
        lookup = playerCountLookup;
    }

    public static boolean supports(String type) {
        PlayerCountLookup current = lookup;
        return current == null || current.supports(type);
    }

    public static int[] lookup(String type, String name) {
        PlayerCountLookup current = lookup;
        return current == null ? null : current.lookup(type, name);
    }

    public static List<ServiceCount> running(String type, String name) {
        PlayerCountLookup current = lookup;
        return current == null ? List.of() : current.running(type, name);
    }
}
