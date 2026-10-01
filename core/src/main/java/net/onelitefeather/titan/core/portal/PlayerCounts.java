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
package net.onelitefeather.titan.core.portal;

import org.jetbrains.annotations.NotNull;

/**
 * Supplies player counts for portal labels. Exactly one provider is active; a real provider
 * replaces the built-in fallback without configuration.
 */
public interface PlayerCounts {

    default boolean supports(@NotNull SourceType type) {
        return true;
    }

    @NotNull
    PlayerCount count(@NotNull SourceType type, @NotNull String name);
}
