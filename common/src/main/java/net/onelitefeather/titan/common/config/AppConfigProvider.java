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
package net.onelitefeather.titan.common.config;

import com.google.gson.Gson;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.theevilreaper.aves.file.GsonFileHandler;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public final class AppConfigProvider {
    private final Path path;
    private final String APP_FILE_NAME = "app.json";
    private final Gson gson;
    private final GsonFileHandler fileHandler;
    private AppConfig appConfig;

    private AppConfigProvider(Path path) {
        this.path = path;
        var typeAdapter = new PositionGsonAdapter();
        this.gson = new Gson().newBuilder()
                .registerTypeAdapter(Pos.class, typeAdapter)
                .registerTypeAdapter(Vec.class, typeAdapter)
                .create();
        this.fileHandler = new GsonFileHandler(this.gson);
        this.loadConfig();
    }

    private void loadConfig() {
        Optional<AppConfigImpl> appConfig = this.fileHandler.load(this.path.resolve(this.APP_FILE_NAME), AppConfigImpl.class);
        if (appConfig.isEmpty()) {
            this.saveConfig(InternalAppConfig.defaultConfig());
        }
        this.appConfig = appConfig.map(AppConfig.class::cast).orElse(InternalAppConfig.defaultConfig());
    }

    public void saveConfig(@NotNull AppConfig config) {
        this.fileHandler.save(this.path.resolve(this.APP_FILE_NAME), config);
        this.loadConfig();
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public static AppConfigProvider create(Path path) {
        return new AppConfigProvider(path);
    }
}
