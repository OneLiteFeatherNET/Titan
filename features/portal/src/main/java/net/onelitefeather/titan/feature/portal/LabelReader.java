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
package net.onelitefeather.titan.feature.portal;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntSupplier;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the source of a portal's label to a {@link LabelReading}. A source the provider cannot
 * serve reads as offline and is reported once per portal and source, not on every refresh.
 */
final class LabelReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LabelReader.class);

    private final PlayerCounts counts;
    private final IntSupplier localPlayers;
    private final Set<String> warned = new HashSet<>();

    LabelReader(PlayerCounts counts, IntSupplier localPlayers) {
        this.counts = counts;
        this.localPlayers = localPlayers;
    }

    LabelReading read(Portal portal) {
        return switch (LabelSource.orDefault(portal)) {
            case LabelSource.Task source -> remote(portal, SourceType.TASK, source.name());
            case LabelSource.Group source -> remote(portal, SourceType.GROUP, source.name());
            case LabelSource.Service source -> remote(portal, SourceType.SERVICE, source.name());
            case LabelSource.Local ignored -> new LabelReading.Local(this.localPlayers.getAsInt());
            case LabelSource.Unknown source ->
                unavailable(portal, String.valueOf(source.type()), "");
        };
    }

    private LabelReading remote(Portal portal, SourceType type, String name) {
        if (!this.counts.supports(type)) {
            return unavailable(portal, type.name().toLowerCase(Locale.ROOT), name);
        }
        return new LabelReading.Remote(this.counts.count(type, name));
    }

    private LabelReading unavailable(Portal portal, String type, String name) {
        if (this.warned.add(portal.id() + '\0' + type + '\0' + name)) {
            LOGGER.warn("Portal label source {} '{}' of portal '{}' is unavailable", type, name, portal.id());
        }
        return new LabelReading.Remote(PlayerCount.NOT_RUNNING);
    }
}
