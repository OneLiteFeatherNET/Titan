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
 * Names every {@code PlatformBeans} bean a not-yet-moved column will need once it lives under
 * {@code features/*}, so each column's own {@code requires}/{@code requiresString} (see the
 * protection spike's {@code package-info.java}) finds a declared provider here instead of failing
 * the "missing dependency" compile-time check. Deliberately only the plain {@code Class<?>} form,
 * not also {@code providesString}: {@code :app} needs no compile-time "missing dependency"
 * suppression for a bean it defines itself, and combining {@code provides} with
 * {@code providesString} on this (unnamed, default-scope) module tripped a code generation bug in
 * avaje-inject-generator 12.7 (malformed, uncompilable {@code @InjectModule} on the generated
 * {@code AppModule} - a missing comma between the two attributes). Plain {@code provides} alone is
 * exactly what {@code requires} on the other end needs for module build ordering. In this wave
 * {@code :app} is still both the assembly and the platform; this declaration moves to
 * {@code runtime} in a later wave together with {@code PlatformBeans}.
 *
 * <p>{@link LobbyItems} is listed because {@code HotbarLobbyItems} (the only bean implementing it)
 * still lives in {@code :app}; once {@code hotbar} moves to its own column, whoever moves it must
 * also move this entry to {@code features/hotbar}'s own {@code provides} - two modules both
 * claiming to provide it is not itself a compile error (avaje-inject-generator only orders
 * modules by {@code provides}/{@code requires}, it does not check the claim against an actual
 * bean), but a stale claim here would silently skip the "missing dependency" compile check and
 * turn a real wiring bug into a runtime {@code BeanScope.build()} failure instead.
 */
@InjectModule(provides = {EventNode.class, Instance.class, LobbySpawn.class, Deliver.class, FeatureFlags.class, Clock.class, Scheduler.class, CommandManager.class, LobbyItems.class})
package net.onelitefeather.titan.app;

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
