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
 * The {@code elytra} column. See {@code docs/lobby-modules.md}, "Wie eine Column
 * Plattform-Beans bekommt" - {@code provides = {LobbyItem.class}} but takes a
 * {@code Provider<LobbyItems>} instead of {@code LobbyItems} directly, to avoid a build-order
 * cycle with {@code hotbarColumn} (see
 * {@link net.onelitefeather.titan.feature.elytra.ElytraModule}).
 */
@InjectModule(
        name = "elytraColumn", requires = {EventNode.class, Scheduler.class, Clock.class, Telemetry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"}, provides = {LobbyItem.class}
)
package net.onelitefeather.titan.feature.elytra;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.telemetry.Telemetry;
