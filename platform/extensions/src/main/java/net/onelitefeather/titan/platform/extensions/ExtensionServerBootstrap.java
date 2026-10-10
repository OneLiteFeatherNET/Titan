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
package net.onelitefeather.titan.platform.extensions;

import java.util.List;
import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.minestom.server.Auth;
import net.minestom.server.extensions.ExtensionManager;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;

/**
 * Starts Minestom through minestom-extensions, so the {@code extensions/} folder is loaded before
 * the server accepts a player. Registered for {@link java.util.ServiceLoader} in
 * {@code META-INF/services}.
 */
public final class ExtensionServerBootstrap implements ServerBootstrap {

    private ExtensionBootstrap bootstrap;

    @Override
    public String name() {
        return "extensions";
    }

    @Override
    public void init(Auth auth) {
        this.bootstrap = auth == null ? ExtensionBootstrap.init() : ExtensionBootstrap.init(auth);
    }

    @Override
    public void start(String host, int port) {
        this.bootstrap.start(host, port);
    }

    @Override
    public List<String> loadedExtensions() {
        // The extension manager only exists after init; asking earlier would throw instead of reporting none.
        if (this.bootstrap == null) {
            return List.of();
        }
        ExtensionManager manager = ExtensionBootstrap.getExtensionManager();
        return ExtensionNames.sorted(manager.getExtensions().stream().map(extension -> extension.getOrigin().getName()).toList());
    }
}
