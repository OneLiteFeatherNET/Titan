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
package net.onelitefeather.titan.runtime.bootstrap;

import io.avaje.inject.PostConstruct;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.server.ServerListPingEvent;
import net.onelitefeather.titan.common.deliver.TitanPlayerLimit;
import net.onelitefeather.titan.core.module.FeatureNode;

/**
 * Reports the configured {@link LobbyPlayerLimit} in every server list ping and announces it to
 * the CloudNet bridge extension, which read its own ping before this bean existed.
 */
@Singleton
final class LobbyPlayerLimitAnnouncer {

    private final EventNode<Event> titan;
    private final LobbyPlayerLimit limit;

    LobbyPlayerLimitAnnouncer(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, LobbyPlayerLimit limit) {
        this.titan = titan;
        this.limit = limit;
    }

    @PostConstruct
    void announce() {
        this.titan.addListener(ServerListPingEvent.class, this.limit::report);
        TitanPlayerLimit.announce(this.limit.maxPlayers());
    }
}
