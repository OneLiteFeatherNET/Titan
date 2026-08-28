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
package net.onelitefeather.titan.common.resourcepack;

import com.google.gson.reflect.TypeToken;
import net.theevilreaper.aves.file.ModernGsonFileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads {@code resource-packs.json} next to the server and turns it into
 * {@link ResourcePackSettings}.
 *
 * <p>A missing file is the normal case and not an error: it yields
 * {@link ResourcePackSettings#disabled()}, and nothing is written to disk. A malformed file is
 * reported and also yields disabled settings, so a typo in the configuration cannot keep players
 * out of the lobby.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class ResourcePackSettingsProvider {

    /** Name of the configuration file, resolved against the server directory. */
    public static final String FILE_NAME = "resource-packs.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackSettingsProvider.class);
    private static final TypeToken<ResourcePackSettings> TYPE = TypeToken.get(ResourcePackSettings.class);

    private ResourcePackSettingsProvider() {
    }

    /**
     * Loads the settings from {@code <directory>/resource-packs.json}.
     *
     * @param directory the directory the server runs in
     * @return the configured settings, or {@link ResourcePackSettings#disabled()} when the file
     *         is absent or cannot be read
     */
    public static ResourcePackSettings load(Path directory) {
        return reload(directory).orElseGet(ResourcePackSettings::disabled);
    }

    /**
     * Reads the settings and reports whether the file could be read at all.
     *
     * <p>{@link #load(Path)} cannot tell "the operator configured no pack" from "the file is
     * missing or broken" - both end up disabled. A reload has to tell them apart: a lobby that
     * is already serving packs must not strip them from every player because someone saved a
     * half-written file. So an absent or unreadable file yields an empty result here, and only
     * a file that actually parsed yields settings - even when those settings configure nothing.
     *
     * @param directory the directory the server runs in
     * @return the parsed settings, or empty when the file is absent or cannot be read
     */
    public static Optional<ResourcePackSettings> reload(Path directory) {
        Path file = directory.resolve(FILE_NAME);
        ResourcePackSettings settings;
        try {
            settings = new ModernGsonFileHandler().load(file, TYPE).orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to read {} - keeping the resource packs that are in effect", file, exception);
            return Optional.empty();
        }
        if (settings == null) {
            LOGGER.debug("No {} found - keeping the resource packs that are in effect", file);
            return Optional.empty();
        }
        if (!settings.enabled()) {
            LOGGER.info("{} configures no pack", file);
            return Optional.of(settings);
        }
        warnAboutCaching(settings);
        return Optional.of(settings);
    }

    /**
     * Logs a warning for every configured pack whose URL does not carry its hash. The client
     * caches downloaded packs by URL, so a pack served from a fixed URL is never re-downloaded
     * after its content changed.
     *
     * @param settings the settings to check
     */
    private static void warnAboutCaching(ResourcePackSettings settings) {
        for (PackSlot slot : PackSlot.values()) {
            ResourcePackDefinition definition = settings.packFor(slot);
            if (definition != null && !definition.contentAddressed()) {
                LOGGER.warn("The {} pack url does not contain its sha1 ({}). Clients cache packs by url, not by hash - serve it as pack-<sha1>.zip or changes will not reach players who already downloaded it.", slot, definition.url());
            }
        }
    }
}
