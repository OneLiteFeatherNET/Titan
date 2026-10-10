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
package net.onelitefeather.titan.runtime;

import java.util.function.Consumer;
import net.minestom.server.Auth;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The order of the boot: the server is initialised before any feature bean exists, and it only
 * starts listening after the features are built, so no player connects to a half-built lobby.
 */
public final class TitanStartup {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanStartup.class);

    private TitanStartup() {
    }

    /**
     * @param installExceptionHandler replaces Minestom's default exception output
     * @param buildScope              builds the feature scope on the initialised server
     */
    public static void run(ServerBootstrap bootstrap, Auth auth, String host, int port, Runnable installExceptionHandler, Consumer<ServerBootstrap> buildScope) {
        LOGGER.info("Server bootstrap: {}", bootstrap.name());
        bootstrap.init(auth);
        installExceptionHandler.run();
        buildScope.accept(bootstrap);
        bootstrap.start(host, port);
    }
}
