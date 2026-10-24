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
package net.onelitefeather.titan.feature.settings;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executor;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.SettingDefault;
import net.onelitefeather.titan.core.settings.SettingKey;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.settings.SettingsStore;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/** Builds the settings column the way the tests need it: fresh objects, real events. */
final class SettingsFixtures {

    static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");

    /** An opt-in setting, off by default. */
    static final BooleanSetting TAG = new BooleanSetting(SettingKey.parse("titan:tag.enabled"), SettingDefault.OPT_IN);

    /** An opt-out setting, on by default. */
    static final BooleanSetting THROW = new BooleanSetting(SettingKey.parse("titan:throw.target"), SettingDefault.OPT_OUT);

    static final SettingsOptions ENABLED = new SettingsOptions(true, true, java.time.Duration.ofMillis(2000), 3);

    static final SettingsOptions DISABLED = new SettingsOptions(false, true, java.time.Duration.ofMillis(2000), 3);

    /** An executor that runs each task at once, on the calling thread. */
    static final Executor SAME_THREAD = Runnable::run;

    private SettingsFixtures() {
    }

    static SettingsRegistry registry(BooleanSetting... settings) {
        SettingsRegistry registry = new SettingsRegistry();
        int index = 0;
        for (BooleanSetting setting : settings) {
            registry.register("column-" + index++, setting);
        }
        return registry;
    }

    static SettingsModule module(Env env, EventNode<Event> titan, SettingsOptions options, SettingsStore store, SettingsRegistry registry, Executor io) {
        return new SettingsModule(titan, Telemetry.noop(), env.process().scheduler(), env.process().command(), store, registry, options, new SettingsMessages(), new AdjustableClock(NOW, ZoneOffset.UTC), io);
    }
}
