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
 * The {@code jumprun} column. Like {@code elytra}, it takes {@code LobbyItems} as a
 * {@code Provider}
 * to avoid a build-order cycle with {@code hotbarColumn}, and {@code LobbyHeightBounds} the same
 * way,
 * because {@code spawnColumn}, which provides it, needs the hotbar too. So only {@code EventNode},
 * {@code LobbySpawn}, {@code LobbyPortals}, the platform's {@code Clock} and its {@code Scheduler},
 * and the {@code Telemetry} are declared as required.
 */
@InjectModule(
        name = "jumprunColumn", requires = {EventNode.class, LobbySpawn.class, LobbyPortals.class, Clock.class, Scheduler.class, Telemetry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"}, provides = {LobbyItem.class, PersistenceUnit.class}
)
package net.onelitefeather.titan.feature.jumprun;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.persistence.PersistenceUnit;
