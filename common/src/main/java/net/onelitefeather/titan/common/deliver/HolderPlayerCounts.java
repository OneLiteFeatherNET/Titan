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
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;

/**
 * {@link PlayerCounts} backed by the CloudNet bridge. It translates the JDK-typed
 * {@link TitanPlayerCountLookup} into the provider-neutral contract; with no bridge installed
 * every source reads as not running.
 */
public final class HolderPlayerCounts implements PlayerCounts {

    @Override
    public boolean supports(SourceType type) {
        return TitanPlayerCountLookup.supports(type.id());
    }

    @Override
    public PlayerCount count(SourceType type, String name) {
        int[] counts = TitanPlayerCountLookup.lookup(type.id(), name);
        return counts == null ? PlayerCount.NOT_RUNNING : new PlayerCount(counts[0], counts[1], true);
    }

    @Override
    public List<ServiceCount> running(SourceType type, String name) {
        return TitanPlayerCountLookup.running(type.id(), name);
    }
}
