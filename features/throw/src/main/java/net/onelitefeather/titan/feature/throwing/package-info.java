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
 * The {@code throw} column: a player with {@code titan.throw} picks up another player and throws
 * them. The target can opt out with the {@code titan:throw.target} setting.
 */
@InjectModule(name = "throwColumn", requires = {EventNode.class, Scheduler.class, Clock.class, Telemetry.class, PermissionService.class, PlayerSettings.class, SettingsRegistry.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"}, provides = {PortalTransferGuard.class})
package net.onelitefeather.titan.feature.throwing;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.PortalTransferGuard;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
