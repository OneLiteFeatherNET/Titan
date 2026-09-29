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

import java.util.Optional;

/**
 * Names the world directory the lobby loads at startup. Implementations are found through
 * {@link java.util.ServiceLoader}, not as beans: the choice is needed before any feature module
 * exists, and a bean would come out empty. An implementation needs a public no-arg constructor.
 */
public interface LobbyWorldChoice {

    /** @return the directory name under {@code worlds/}; empty for the default world */
    Optional<String> worldName();
}
