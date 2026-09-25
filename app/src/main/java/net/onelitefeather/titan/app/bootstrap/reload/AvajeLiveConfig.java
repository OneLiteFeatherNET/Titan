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
package net.onelitefeather.titan.app.bootstrap.reload;

import io.avaje.config.Config;
import io.avaje.config.ModificationEvent;
import java.util.Map;
import java.util.Set;

/**
 * Production {@link LiveConfig}: reads the live configuration through the static
 * {@code io.avaje.config.Config} facade and applies a diff through exactly one
 * {@code Config.eventBuilder(name)} - put every changed/added key, remove every deleted one, then
 * publish - so a reload (or a revert) is one atomic step, never a half-applied one. See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1.
 */
final class AvajeLiveConfig implements LiveConfig {

    private static final String RELOAD_EVENT = "reload";
    private static final String REVERT_EVENT = "reload-revert";

    @Override
    public Map<String, String> currentValues() {
        return FlatConfigValues.of(Config.asConfiguration().asProperties());
    }

    @Override
    public void apply(ConfigDiff diff) {
        publish(RELOAD_EVENT, diff.puts(), diff.removals());
    }

    @Override
    public void applyRevert(Map<String, String> puts, Set<String> removals) {
        publish(REVERT_EVENT, puts, removals);
    }

    private static void publish(String eventName, Map<String, String> puts, Set<String> removals) {
        ModificationEvent.Builder builder = Config.eventBuilder(eventName).putAll(puts);
        for (String key : removals) {
            builder.remove(key);
        }
        builder.publish();
    }
}
