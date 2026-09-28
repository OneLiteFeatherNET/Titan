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
 * The {@code admin} column: {@code name} is explicit and distinct from the bean class
 * {@link net.onelitefeather.titan.feature.admin.AdminCommands}.
 * {@link net.minestom.server.command.CommandManager}
 * has neither a qualifier nor a generic parameter, so the plain {@code requires} form alone
 * satisfies the compile-time "missing dependency" check and feeds
 * {@code AvajeModule.requiresBeans()} for build ordering - no {@code requiresString} entry is
 * needed. See {@code docs/lobby-modules.md}, "Wie eine Column Plattform-Beans bekommt", for the
 * full spike result.
 */
@InjectModule(name = "adminColumn", requires = {CommandManager.class})
package net.onelitefeather.titan.feature.admin;

import io.avaje.inject.InjectModule;
import net.minestom.server.command.CommandManager;
