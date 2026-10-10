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
 * The {@code portal} column: sends a player to a CloudNet task when they walk or fly through a
 * portal of the lobby world. See {@code docs/lobby-modules.md}, "Portale".
 */
@InjectModule(name = "portalColumn", requires = {EventNode.class, LobbyPortals.class, Deliver.class, PermissionService.class, Clock.class, Instance.class, Scheduler.class, Telemetry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.portal;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.telemetry.Telemetry;
