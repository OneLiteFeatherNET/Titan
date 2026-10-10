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
/**
 * The {@code respawn} column. See {@code docs/lobby-modules.md}, "Wie eine Column
 * Plattform-Beans bekommt" - like {@code spawn}, it injects {@code LobbyItems} directly, since it
 * contributes no {@code LobbyItem} of its own.
 */
@InjectModule(name = "respawnColumn", requires = {EventNode.class, LobbyItems.class, Telemetry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.respawn;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.telemetry.Telemetry;
