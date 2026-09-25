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

import io.avaje.inject.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.common.config.ConfigValues;

/**
 * Lets a player tickle another player by attacking them while holding a feather in either hand:
 * broadcasts a message to the instance and applies a per-player cooldown.
 *
 * <p>Its observable behaviour - including today's two known cooldown bugs - is unchanged from
 * before this module existed. See {@link TickleAttackHandler} and {@link TickleCooldownRule} for
 * the implementation, and {@link TickleSettings} for this module's own configuration key and
 * validation.
 */
@Singleton
@Priority(600)
public final class TickleModule implements LobbyModule {

    private final Clock clock;

    /**
     * Creates a module backed by {@code clock} - the platform's {@code Clock} bean is the system
     * clock in production, so a test can control what "now" is instead of the module depending on
     * {@link System#currentTimeMillis()}.
     *
     * @param clock the clock to read the current time from
     */
    @Inject
    public TickleModule(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String id() {
        return "tickle";
    }

    @Override
    public void enable(ModuleContext context) {
        Duration cooldown = TickleSettings.cooldown(ConfigValues.longValue(TickleSettings.COOLDOWN_KEY));
        context.listen(EntityAttackEvent.class, new TickleAttackHandler(this.clock, cooldown.toMillis()));
    }
}
