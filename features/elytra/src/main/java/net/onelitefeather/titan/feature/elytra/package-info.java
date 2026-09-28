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
 * The {@code elytra} column: {@code name} is explicit and distinct from the bean
 * class {@link net.onelitefeather.titan.feature.elytra.ElytraModule}, so a later wave's
 * "expected column loaded" check can read it without ambiguity. Both {@code requires} forms
 * are needed, for two different checks: {@code requiresString}, keyed exactly like the processor's
 * own "No dependency provided for ...EventNode&lt;...Event&gt;:titan" error message, satisfies the
 * per-module compile-time check (plain {@code requires = {EventNode.class}} alone does not - it
 * drops the {@code @Named("titan")} qualifier and the generic parameter); {@code requires} itself
 * additionally feeds {@code AvajeModule.requiresBeans()}, which is what orders this module after
 * {@code :app}'s (later {@code runtime}'s) at {@code BeanScope} build time - dropping it lets
 * Avaje build this column before its platform beans exist, failing at runtime instead of compile
 * time. {@code Scheduler} needs only the plain {@code Class<?>} form, since it is neither generic
 * nor {@code @Named}. {@code provides = {LobbyItem.class}} declares that
 * {@link net.onelitefeather.titan.feature.elytra.ElytraLobbyItems} contributes {@code LobbyItem}
 * beans to the platform-wide list {@code HotbarLobbyItems} collects.
 *
 * <p>{@code LobbyItems} itself is deliberately absent from both {@code requires} forms:
 * {@link net.onelitefeather.titan.feature.elytra.ElytraModule} injects it as a
 * {@code jakarta.inject.Provider}, which Avaje resolves lazily rather than at this module's build
 * time. Declaring it here would recreate the build-order cycle a {@code requires} on
 * {@code LobbyItems} and a {@code provides} of {@code LobbyItem} on the same column would
 * otherwise cause - {@code hotbarColumn} needs every {@code LobbyItem} first (to build the list
 * this column contributes to), while an eager {@code LobbyItems} dependency here would need
 * {@code hotbarColumn} first. See {@code docs/lobby-modules.md}, "Wie eine Column
 * Plattform-Beans bekommt", for the full spike result and this rule.
 */
@InjectModule(
        name = "elytraColumn", requires = {EventNode.class, Scheduler.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"}, provides = {LobbyItem.class}
)
package net.onelitefeather.titan.feature.elytra;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.module.item.LobbyItem;
