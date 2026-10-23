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
package net.onelitefeather.titan.core.ambience;

import java.util.List;

/**
 * The ambience zones of the active lobby world. A dedicated type, like {@code LobbyPortals}, so the
 * DI container has an unambiguous bean to wire.
 */
@FunctionalInterface
public interface LobbyAmbiences {

    /** Unmodifiable; empty, never {@code null}, if the world has no zones. */
    List<AmbienceZone> zones();
}
