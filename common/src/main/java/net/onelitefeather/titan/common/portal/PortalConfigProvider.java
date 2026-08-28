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
package net.onelitefeather.titan.common.portal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.theevilreaper.aves.file.ModernGsonFileHandler;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads {@value PortalConfig#PORTAL_FILE_NAME} from the server directory, the same way
 * {@link net.onelitefeather.titan.common.config.AppConfigProvider} loads {@code app.json}: a Gson
 * with the aves position adapter registered, an aves file handler, and a default written out when
 * the file does not exist yet (US-7.02).
 *
 * <p>It uses the non-deprecated {@link ModernGsonFileHandler} rather than the {@code
 * GsonFileHandler} the older providers still use, so this file does not have to be touched again
 * when that class is removed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class PortalConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalConfigProvider.class);

    private static final TypeToken<PortalConfigImpl> TYPE = TypeToken.get(PortalConfigImpl.class);

    private final Path file;
    private final ModernGsonFileHandler fileHandler;
    private PortalConfig portalConfig = PortalConfig.defaultConfig();

    private PortalConfigProvider(Path path) {
        this.file = path.resolve(PortalConfig.PORTAL_FILE_NAME);
        var typeAdapter = new PositionGsonAdapter();
        Gson gson = new Gson().newBuilder().setPrettyPrinting().registerTypeAdapter(Pos.class, typeAdapter).registerTypeAdapter(Vec.class, typeAdapter).create();
        this.fileHandler = new ModernGsonFileHandler(gson);
        this.loadConfig();
    }

    /**
     * Creates a provider reading from the given directory.
     *
     * @param path the directory holding {@value PortalConfig#PORTAL_FILE_NAME}
     * @return the provider, with the configuration already loaded
     */
    public static PortalConfigProvider create(Path path) {
        return new PortalConfigProvider(path);
    }

    /**
     * Returns the loaded configuration.
     *
     * @return the portal configuration
     */
    public PortalConfig getPortalConfig() {
        return this.portalConfig;
    }

    /**
     * Writes the given configuration to disk and reloads it.
     *
     * @param config the configuration to persist
     */
    public void saveConfig(PortalConfig config) {
        this.fileHandler.save(this.file, (PortalConfigImpl) config, TYPE);
        this.loadConfig();
    }

    private void loadConfig() {
        Optional<PortalConfigImpl> loaded;
        try {
            loaded = this.fileHandler.load(this.file, TYPE);
        } catch (RuntimeException exception) {
            // A malformed file must not take the lobby down, and it must not look like "no
            // portals configured" either - that reads as an empty file rather than a broken one.
            LOGGER.error("Unable to read {}; no portal will be active until the file parses", this.file, exception);
            this.portalConfig = PortalConfig.defaultConfig();
            return;
        }
        if (loaded.isEmpty()) {
            this.portalConfig = PortalConfig.defaultConfig();
            this.saveDefault();
            return;
        }
        this.portalConfig = loaded.get();
    }

    private void saveDefault() {
        this.fileHandler.save(this.file, PortalConfigImpl.DEFAULT, TYPE);
    }
}
