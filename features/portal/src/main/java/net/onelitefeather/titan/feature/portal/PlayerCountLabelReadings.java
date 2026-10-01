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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.IntSupplier;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.SourceType;

/**
 * Reads a label's source from the {@link PlayerCounts} provider, or from this lobby for
 * {@code local}.
 */
@Singleton
final class PlayerCountLabelReadings implements LabelReadings {

    private static final LabelReading NOT_RUNNING = new LabelReading.Remote(PlayerCount.NOT_RUNNING);

    private final PlayerCounts counts;
    private final IntSupplier localOnline;

    @Inject
    PlayerCountLabelReadings(PlayerCounts counts) {
        // Looked up per call: the process may not exist yet when this bean is built.
        this(counts, () -> MinecraftServer.getConnectionManager().getOnlinePlayerCount());
    }

    PlayerCountLabelReadings(PlayerCounts counts, IntSupplier localOnline) {
        this.counts = counts;
        this.localOnline = localOnline;
    }

    @Override
    public LabelReading read(Portal portal, PortalLabel label) {
        LabelSource source = label.source() != null ? label.source() : new LabelSource.Task(portal.task());
        return switch (source) {
            case LabelSource.Local local -> new LabelReading.Local(this.localOnline.getAsInt());
            case LabelSource.Task task -> remote(SourceType.TASK, task.name());
            case LabelSource.Group group -> remote(SourceType.GROUP, group.name());
            case LabelSource.Service service -> remote(SourceType.SERVICE, service.name());
            case LabelSource.Unknown unknown -> NOT_RUNNING;
        };
    }

    private LabelReading remote(SourceType type, String name) {
        if (name == null || !this.counts.supports(type)) {
            return NOT_RUNNING;
        }
        return new LabelReading.Remote(this.counts.count(type, name));
    }
}
