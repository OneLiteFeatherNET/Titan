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
package net.onelitefeather.titan.feature.season;

import jakarta.inject.Singleton;
import net.minestom.server.MinecraftServer;

/** Default {@link ServerStop}, the same way {@code /stop} does it. */
@Singleton
final class MinecraftServerStop implements ServerStop {

    @Override
    public void stop() {
        // Runs on a separate thread: stopCleanly() waits for the tick thread this is called from.
        Thread.ofPlatform().name("titan-stop").start(() -> {
            MinecraftServer.stopCleanly();
            System.exit(0);
        });
    }
}
