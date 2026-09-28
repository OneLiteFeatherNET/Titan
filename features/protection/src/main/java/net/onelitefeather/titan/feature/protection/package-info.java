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
 * The {@code protection} column (D2 spike): {@code name} is explicit and distinct from the bean
 * class {@link net.onelitefeather.titan.feature.protection.ProtectionModule}, so a later wave's
 * "expected column loaded" check (D5) can read it without ambiguity. Both {@code requires} forms
 * are needed, for two different checks: {@code requiresString}, keyed exactly like the processor's
 * own "No dependency provided for ...EventNode&lt;...Event&gt;:titan" error message, satisfies the
 * per-module compile-time check (plain {@code requires = {EventNode.class}} alone does not - it
 * drops the {@code @Named("titan")} qualifier and the generic parameter); {@code requires} itself
 * additionally feeds {@code AvajeModule.requiresBeans()}, which is what orders this module after
 * {@code :app}'s (later {@code runtime}'s) at {@code BeanScope} build time - dropping it lets
 * Avaje build this column before its platform bean exists, failing at runtime instead of compile
 * time. See {@code docs/lobby-modules.md}, "Wie eine Column Plattform-Beans bekommt", for the full
 * spike result.
 */
@InjectModule(name = "protectionColumn", requires = {EventNode.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.protection;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
