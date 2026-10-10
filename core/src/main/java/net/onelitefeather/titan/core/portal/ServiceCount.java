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
 * One running service as the provider reports it.
 *
 * @param name   the service name, e.g. {@code Lobby-1}
 * @param online players currently on the service
 * @param max    the player limit; {@code 0} while the service has not announced one
 */
public record ServiceCount(@NotNull String name, int online, int max) {
}
