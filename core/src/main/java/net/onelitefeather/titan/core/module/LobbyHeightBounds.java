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
package net.onelitefeather.titan.core.module;

/**
 * The lobby's vertical bounds, read live on every call rather than captured once, since the
 * configured values can change while the server runs.
 *
 * <p>A dedicated type rather than two bare {@code Integer} values, which would be ambiguous beans
 * for the DI container to wire; the owning column provides it so others never read its
 * configuration section directly.
 */
public interface LobbyHeightBounds {

    /** Lowest {@code y} still inside the lobby. */
    int minHeight();

    /** Highest {@code y} still inside the lobby. */
    int maxHeight();
}
