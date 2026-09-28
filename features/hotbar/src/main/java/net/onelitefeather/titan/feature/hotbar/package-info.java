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
 * The {@code hotbar} column: {@code name} is explicit and distinct from the bean class
 * {@link net.onelitefeather.titan.feature.hotbar.HotbarLobbyItems}. Both {@code requires} forms
 * are needed for the shared {@code titan} {@link net.minestom.server.event.EventNode}, for two
 * different checks: {@code requiresString}, keyed exactly like the processor's own "No dependency
 * provided for ...EventNode&lt;...Event&gt;:titan" error message, satisfies the per-module
 * compile-time check; {@code requires} itself additionally feeds
 * {@code AvajeModule.requiresBeans()}, which orders this module after {@code :app}'s (later
 * {@code runtime}'s) at {@code BeanScope} build time. {@code LobbyItem} has neither a qualifier
 * nor a generic parameter, so the {@code List<LobbyItem>} this column's constructor injects needs
 * no {@code requiresString} entry of its own; it is empty until another column contributes an
 * item. See {@code docs/lobby-modules.md}, "Wie eine Column Plattform-Beans bekommt", for the full
 * spike result.
 */
@InjectModule(name = "hotbarColumn", provides = {LobbyItems.class}, requires = {EventNode.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.hotbar;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.core.module.item.LobbyItems;
