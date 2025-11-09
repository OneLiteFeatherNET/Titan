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
package net.onelitefeather.titan.app.luckperms;


import me.lucko.luckperms.minestom.LPMinestomBootstrap;
import me.lucko.luckperms.minestom.app.LuckPermsApplication;

public final class MinestomLoader {
    private static final LuckPermsApplication APPLICATION = new LuckPermsApplication();
    private static MinestomLoader instance;
    private final LPMinestomBootstrap plugin;

    public MinestomLoader() {
        this.plugin = new LPMinestomBootstrap(APPLICATION);
    }

    public MinestomLoader registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this.plugin::onDisable, "luckperms-shutdown-hook"));
        return this;
    }

    public MinestomLoader load() {
        this.plugin.onLoad();
        return this;
    }

    public void start() {
        this.plugin.onEnable();
    }

    public static MinestomLoader get() {
        MinestomLoader localInstance = instance;
        if (localInstance != null) {
            return localInstance;
        }
        synchronized (MinestomLoader.class) {
            if (instance == null) {
                instance = new MinestomLoader();
            }
            return instance;
        }
    }
}
