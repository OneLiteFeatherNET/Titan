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

import java.util.Map;

/**
 * Builds a fresh, fully-resolved flat view of the configuration whenever {@link #load()} is called.
 *
 * <p>The production implementation (wired in a later wave) builds a brand-new
 * {@code io.avaje.config.Configuration} via {@code Configuration.builder().includeResourceLoading()
 * .build()} - the same pipeline the lobby uses on startup, so files, profiles, {@code CONFIG_FILE}
 * and env/system-property overrides are all re-read and re-ranked exactly like a fresh start (see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1). A test hands in a
 * fake backed by a plain {@link Map} instead.
 */
@FunctionalInterface
public interface ConfigSnapshotSource {

    /**
     * @return the fresh configuration's flat {@code key -> value} view
     * @throws ConfigSnapshotException if building the fresh snapshot failed, e.g. a syntactically
     *                                 broken configuration file
     */
    Map<String, String> load();
}
