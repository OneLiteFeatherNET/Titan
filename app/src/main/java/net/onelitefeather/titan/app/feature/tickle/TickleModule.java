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
import net.onelitefeather.titan.app.module.FeatureNode;

/**
 * Lets a player tickle another player by attacking them while holding a feather in either hand:
 * broadcasts a message to the instance and applies a per-player cooldown.
 *
 * <p>Its observable behaviour - including today's two known cooldown bugs - is unchanged from
 * before this module existed. See {@link TickleAttackHandler} and {@link TickleCooldownRule} for
 * the implementation, and {@link TickleSettings} for this module's own configuration key and
 * validation.
 *
 * <p>{@link #start()} reads {@link TickleSettings#COOLDOWN_KEY} exactly once, through
 * {@link TickleSettings#cooldownMillis(String)}'s strict validation, purely to abort startup on
 * an invalid value (unchanged behaviour from {@code avaje-config-facade}); the result is
 * discarded. {@link TickleAttackHandler} reads the live value itself, on every attack, via
 * {@code Config.getLong(TickleSettings.COOLDOWN_KEY)} - see {@code openspec/changes/
 * config-reload-feature-flags/design.md}, decision 1 - without re-validating it: configuration is
 * validated only at startup (see {@code refactor/drop-runtime-fallback}).
 *
 * <p>An {@code @Singleton} bean (see
 * {@code openspec/changes/dissolve-module-platform/design.md}, decision 1): {@link #start()}
 * attaches this feature's own {@link FeatureNode} once the container builds this bean, and
 * {@link #stop()} detaches it again when the container is closed.
 */
@Singleton
public final class TickleModule {

    static final int EVENT_PRIORITY = 600;

    private final EventNode<Event> titan;
    private final Clock clock;
    private FeatureNode node;

    /**
     * Creates a module backed by {@code clock} - the platform's {@code Clock} bean is the system
     * clock in production, so a test can control what "now" is instead of the module depending on
     * {@link System#currentTimeMillis()}.
     *
     * @param titan the shared event node this feature's own node attaches under
     * @param clock the clock to read the current time from
     */
    @Inject
    public TickleModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Clock clock) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostConstruct
    void start() {
        // Abort startup on an invalid value (unchanged behaviour); the parsed value itself is not
        // kept - TickleAttackHandler reads the live value again on every attack.
        Config.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis);
        this.node = FeatureNode.attach(this.titan, "tickle", EVENT_PRIORITY).on(EntityAttackEvent.class, new TickleAttackHandler(this.clock));
    }

    @PreDestroy
    void stop() {
        this.node.close();
    }
}
