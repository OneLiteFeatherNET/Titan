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
package net.onelitefeather.titan.app;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomBridgeExtension;
import net.minestom.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;


public class TitanApplication {

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        me.lucko.luckperms.minestom.loader.MinestomLoader.get().load().registerShutdownHook().start();
        Titan titan = new Titan();
        titan.initialize();

        // CloudNet only provides its runtime when the service is launched through
        // its wrapper, which creates a ".wrapper" directory in the working
        // directory. When running standalone (local, tests, AOT training) there
        // is no CloudNet to bind to, so skip the bridge to avoid failing startup.
        if (Files.isDirectory(Path.of(".wrapper"))) {
            InjectionLayer.ext().instance(MinestomBridgeExtension.class).onLoad();
        }

        minecraftServer.start("localhost", 25565);

        // AOT training aid: when -Dtitan.aot.trainSeconds=<n> is set, shut down
        // cleanly after the server has started so the JVM exit writes the AOT
        // configuration/cache. No effect in normal operation.
        Long aotTrainSeconds = Long.getLong("titan.aot.trainSeconds");
        if (aotTrainSeconds != null) {
            Thread.ofVirtual().name("titan-aot-trainer").start(() -> {
                try {
                    Thread.sleep(aotTrainSeconds * 1000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
            });
        }
    }
}
