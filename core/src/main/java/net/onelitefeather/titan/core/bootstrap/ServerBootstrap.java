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
package net.onelitefeather.titan.core.bootstrap;

import java.util.List;
import net.minestom.server.Auth;

/**
 * How the lobby starts its Minestom server. A variant gets exactly one of these: plain Minestom,
 * or Minestom with the extension loader when the variant ships the {@code platform/extensions}
 * module.
 */
public interface ServerBootstrap {

    /** Names the bootstrap in the start log and in the variant's expected modules. */
    String name();

    /** Initialises the server; {@code auth == null} keeps Minestom's default authentication. */
    void init(Auth auth);

    /** Binds the initialised server. */
    void start(String host, int port);

    /** Names of the extensions this bootstrap loaded, in a stable order; empty without a loader. */
    List<String> loadedExtensions();
}
