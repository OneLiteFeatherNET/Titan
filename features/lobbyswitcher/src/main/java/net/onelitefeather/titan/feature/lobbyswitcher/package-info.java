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
 * The {@code lobbyswitcher} column: a hotbar item that lists the running lobbies of this task and
 * sends the player to one of them. See {@code openspec/changes/lobby-switcher}.
 */
@InjectModule(name = "lobbyswitcherColumn", requires = {Deliver.class, EventNode.class, FeatureFlags.class, LobbyIdentities.class, PlayerCounts.class, Scheduler.class, Telemetry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"}, provides = {LobbyItem.class})
package net.onelitefeather.titan.feature.lobbyswitcher;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.lobby.LobbyIdentities;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.telemetry.Telemetry;
