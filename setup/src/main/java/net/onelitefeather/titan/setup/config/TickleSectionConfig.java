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
package net.onelitefeather.titan.setup.config;

/**
 * Setup-local mirror of the lobby's {@code tickle} config section (id {@code "tickle"}).
 * <p>
 * The setup server does not depend on {@code app}, so it cannot reuse the real feature module's
 * config record; this record duplicates the single field the {@code app} command edits.
 *
 * @param cooldownMillis the cooldown between two tickles, in milliseconds
 */
public record TickleSectionConfig(long cooldownMillis) {

    /**
     * The defaults used when {@code app.json} has no {@code tickle} section yet, matching the
     * lobby's own defaults (see {@code InternalAppConfig}).
     */
    public static final TickleSectionConfig DEFAULTS = new TickleSectionConfig(4000L);
}
