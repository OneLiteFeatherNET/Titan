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
 * The {@code spawn} column. See {@code docs/lobby-modules.md}, "Wie eine Column
 * Plattform-Beans bekommt" - injects {@code LobbyItems} directly, since it contributes no
 * {@code LobbyItem} of its own.
 */
@InjectModule(name = "spawnColumn", provides = {LobbyHeightBounds.class, SpawnReturn.class}, requires = {Instance.class, LobbySpawn.class, EventNode.class, LobbyItems.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.spawn;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.module.SpawnReturn;
import net.onelitefeather.titan.core.module.item.LobbyItems;
