/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

public class TitanApplication {

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        Titan titan = new Titan();
        titan.initialize();
        InjectionLayer.ext().instance(MinestomBridgeExtension.class).onLoad();

        minecraftServer.start("localhost", 25565);
    }
}
