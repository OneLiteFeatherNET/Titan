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
 * The {@code sit} column: {@code name} is explicit and distinct from the bean class
 * {@link net.onelitefeather.titan.feature.sit.SitModule}. Both {@code requires} forms are needed:
 * {@code requiresString} satisfies the per-module compile-time check for the qualified, generic
 * {@code EventNode<Event>} platform bean; plain {@code requires} feeds the generated module's
 * {@code requiresBeans()}, which orders this module after the one providing that bean.
 */
@InjectModule(name = "sitColumn", requires = {EventNode.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
package net.onelitefeather.titan.feature.sit;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
