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
 * Names the bean {@code :app}'s own module ({@code PlatformBeans}) hands to every column: the
 * shared {@code @Named("titan") EventNode<Event>} the protection spike's
 * {@code features/protection} module declares as {@code requires} (D2). Deliberately only the
 * plain {@code Class<?>} form, not also {@code providesString}: {@code :app} needs no compile-time
 * "missing dependency" suppression for a bean it defines itself, and combining {@code provides}
 * with {@code providesString} on this (unnamed, default-scope) module tripped a code generation bug
 * in avaje-inject-generator 12.7 (malformed, uncompilable {@code @InjectModule} on the generated
 * {@code AppModule} - a missing comma between the two attributes). Plain {@code provides} alone is
 * exactly what {@code requires} on the other end needs for module build ordering. In this wave
 * {@code :app} is still both the assembly and the platform; this declaration moves to
 * {@code runtime} in a later wave together with {@code PlatformBeans}.
 */
@InjectModule(provides = {EventNode.class})
package net.onelitefeather.titan.app;

import io.avaje.inject.InjectModule;
import net.minestom.server.event.EventNode;
