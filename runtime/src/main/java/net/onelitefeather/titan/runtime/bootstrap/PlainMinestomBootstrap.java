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
package net.onelitefeather.titan.runtime.bootstrap;

import java.util.List;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;

/** Plain Minestom without an extension loader, used when a variant ships no bootstrap platform. */
public final class PlainMinestomBootstrap implements ServerBootstrap {

    private MinecraftServer server;

    @Override
    public String name() {
        return "minestom";
    }

    @Override
    public void init(Auth auth) {
        this.server = auth == null ? MinecraftServer.init() : MinecraftServer.init(auth);
    }

    @Override
    public void start(String host, int port) {
        this.server.start(host, port);
    }

    @Override
    public List<String> loadedExtensions() {
        return List.of();
    }
}
