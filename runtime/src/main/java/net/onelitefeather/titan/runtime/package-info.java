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
 * Names every {@code PlatformBeans} bean a column under {@code features/*} needs, so each column's
 * own {@code requires}/{@code requiresString} (see the protection spike's
 * {@code package-info.java}) finds a declared provider here instead of failing the "missing
 * dependency" compile-time check. Deliberately only the plain {@code Class<?>} form, not also
 * {@code providesString}: {@code runtime} needs no compile-time "missing dependency" suppression
 * for a bean it defines itself, and combining {@code provides} with {@code providesString} on this
 * (unnamed, default-scope) module tripped a code generation bug in avaje-inject-generator 12.7
 * (malformed, uncompilable {@code @InjectModule} on the generated module class - a missing comma
 * between the two attributes). Plain {@code provides} alone is exactly what {@code requires} on the
 * other end needs for module build ordering.
 *
 * <p>No {@code requires}: every column that used to inject {@link LobbyItems}
 * ({@code spawn}/{@code respawn}/{@code elytra}) has moved out to {@code features/*}, and
 * {@code runtime}'s own main code no longer injects it anywhere.
 */
@InjectModule(provides = {EventNode.class, Instance.class, LobbySpawn.class, Deliver.class, FeatureFlags.class, Clock.class, Scheduler.class, CommandManager.class, PermissionService.class})
package net.onelitefeather.titan.runtime;

import io.avaje.inject.InjectModule;
import java.time.Clock;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.LobbySpawn;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.permission.PermissionService;
