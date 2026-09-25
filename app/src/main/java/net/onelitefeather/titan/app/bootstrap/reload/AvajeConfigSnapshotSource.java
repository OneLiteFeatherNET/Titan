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

import io.avaje.config.Configuration;
import java.util.Map;

/**
 * Production {@link ConfigSnapshotSource}: builds a brand-new, fully-resolved
 * {@code Configuration} via {@code Configuration.builder().includeResourceLoading().build()} - the
 * same loading pipeline the lobby uses on startup (files, profiles, {@code CONFIG_FILE}, env and
 * system-property overrides, all re-ranked exactly like a fresh start) - see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1.
 */
final class AvajeConfigSnapshotSource implements ConfigSnapshotSource {

    @Override
    public Map<String, String> load() {
        try {
            Configuration configuration = Configuration.builder().includeResourceLoading().build();
            return FlatConfigValues.of(configuration.asProperties());
        } catch (RuntimeException failure) {
            throw ConfigFailureDetails.toSnapshotException(failure);
        }
    }
}
