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
package net.onelitefeather.titan.app.feature.tickle;

import io.avaje.config.Config;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.Objects;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.onelitefeather.titan.core.module.FeatureNode;

/**
 * Lets a player tickle another player by attacking them while holding a feather in either hand:
 * broadcasts a message to the instance and applies a per-player cooldown.
 */
@Singleton
public final class TickleModule {

    static final int EVENT_PRIORITY = 600;

    private static final String ID = "tickle";

    private final EventNode<Event> titan;
    private final Clock clock;
    private FeatureNode node;

    @Inject
    public TickleModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Clock clock) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostConstruct
    void start() {
        // Abort startup on an invalid value; the parsed value itself is discarded -
        // TickleAttackHandler reads the live value again on every attack.
        Config.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis);
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(EntityAttackEvent.class, new TickleAttackHandler(this.clock));
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }
}
