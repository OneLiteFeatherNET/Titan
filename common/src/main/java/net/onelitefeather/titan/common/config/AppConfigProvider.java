/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.config;

import com.google.gson.Gson;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.theevilreaper.aves.file.GsonFileHandler;
import net.theevilreaper.aves.file.gson.KeyGsonAdapter;
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
        this.gson = new Gson().newBuilder().registerTypeAdapter(Pos.class, typeAdapter).registerTypeAdapter(Vec.class, typeAdapter).registerTypeHierarchyAdapter(Key.class, KeyGsonAdapter.create()).create();
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
