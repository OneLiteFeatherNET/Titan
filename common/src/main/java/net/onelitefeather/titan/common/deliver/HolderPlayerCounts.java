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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link PlayerCounts} backed by the CloudNet bridge. It translates the JDK-typed
 * {@link TitanPlayerCountLookup} into the provider-neutral contract; with no bridge installed
 * every source reads as not running.
 *
 * <p>A {@link LinkageError} from the bridge's classloader is not a {@code RuntimeException}, so
 * the callers' fallbacks would miss it. It is logged (first of its kind as a warning, later ones
 * as DEBUG) and rethrown as an {@link IllegalStateException}.
 */
public final class HolderPlayerCounts implements PlayerCounts {

    private static final Logger LOGGER = LoggerFactory.getLogger(HolderPlayerCounts.class);

    // Reads run on a virtual thread, not on the tick thread.
    private final Set<String> failureKinds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean supports(SourceType type) {
        return TitanPlayerCountLookup.supports(type.id());
    }

    @Override
    public PlayerCount count(SourceType type, String name) {
        int[] counts = guarded(type, name, () -> TitanPlayerCountLookup.lookup(type.id(), name));
        return counts == null ? PlayerCount.NOT_RUNNING : new PlayerCount(counts[0], counts[1], true);
    }

    @Override
    public List<ServiceCount> running(SourceType type, String name) {
        List<Map<String, Object>> rows = guarded(type, name, () -> TitanPlayerCountLookup.running(type.id(), name));
        return rows.stream().map(HolderPlayerCounts::serviceCount).toList();
    }

    private static ServiceCount serviceCount(Map<String, Object> row) {
        return new ServiceCount((String) row.get(PlayerCountLookup.NAME), ((Number) row.get(PlayerCountLookup.ONLINE)).intValue(), ((Number) row.get(PlayerCountLookup.MAX)).intValue());
    }

    private <T> T guarded(SourceType type, String name, Supplier<T> read) {
        try {
            T result = read.get();
            this.failureKinds.clear();
            return result;
        } catch (LinkageError e) {
            if (this.failureKinds.add(e.getClass().getName())) {
                LOGGER.warn("The CloudNet bridge is not usable for {} '{}': {}", type.id(), name, e.toString(), e);
            } else {
                LOGGER.debug("The CloudNet bridge is not usable for {} '{}'", type.id(), name, e);
            }
            throw new IllegalStateException("CloudNet bridge not usable: " + e, e);
        }
    }
}
