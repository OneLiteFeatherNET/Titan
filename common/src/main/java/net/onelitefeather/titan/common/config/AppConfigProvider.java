package net.onelitefeather.titan.common.config;

import com.google.gson.Gson;
import de.icevizion.aves.file.GsonFileHandler;
import de.icevizion.aves.file.gson.PositionGsonAdapter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
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
            this.saveConfig(this.path.resolve(this.APP_FILE_NAME), InternalAppConfig.defaultConfig());
        }
        this.appConfig = appConfig.map(AppConfig.class::cast).orElse(InternalAppConfig.defaultConfig());
    }

    public void saveConfig(@NotNull Path path, @NotNull AppConfig config) {
        this.fileHandler.save(path, config);
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public static AppConfigProvider create(Path path) {
        return new AppConfigProvider(path);
    }
}
