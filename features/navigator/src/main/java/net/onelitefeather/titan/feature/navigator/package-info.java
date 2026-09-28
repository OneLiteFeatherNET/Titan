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
 * The {@code navigator} column: {@code name} is explicit and distinct from the bean class
 * {@link net.onelitefeather.titan.feature.navigator.NavigatorModule}. {@code requires} carries
 * every platform type the constructor injects ({@code EventNode<Event>}, {@code Deliver},
 * {@code FeatureFlags}); only {@code EventNode} is generic and {@code @Named}, so only it also
 * needs the matching {@code requiresString} entry. See {@code docs/lobby-modules.md}, "Wie eine
 * Column Plattform-Beans bekommt", for why both forms of {@code EventNode} are needed together.
 *
 * <p>{@code provides = {LobbyItem.class}}: {@link net.onelitefeather.titan.feature.navigator.
 * NavigatorItems} contributes the navigator feather as a {@code @Bean LobbyItem}, collected by
 * whichever bean requires {@code List<LobbyItem>} (today {@code HotbarLobbyItems} in {@code :app};
 * once {@code hotbar} becomes its own column, that column's {@code requires} finds this provider
 * declared here instead of failing the "missing dependency" compile-time check).
 */
@InjectModule(name = "navigatorColumn", requires = {EventNode.class, Deliver.class, FeatureFlags.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"}, provides = {LobbyItem.class})
package net.onelitefeather.titan.feature.navigator;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.module.item.LobbyItem;
