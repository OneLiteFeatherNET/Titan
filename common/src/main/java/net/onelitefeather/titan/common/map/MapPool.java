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
package net.onelitefeather.titan.common.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The map pool is responsible for managing the available maps. It will load all
 * maps data from the given path and store them. It would not load the map
 * itself over a chunk loader instance. This behavior is handled by another class.
 * <p>
 * Which of the maps is the active one is decided by the system property
 * {@value #LOBBY_MAP_PROPERTY}, and it is decided by that property alone: the amount of worlds
 * below the map directory does not change how the property is read. A world that is named there but
 * is not present is a typo and not a reason to refuse the start, so it is reported by name, next to
 * the names that were actually found, and the pool falls back to {@value #DEFAULT_MAP_NAME}.
 * </p>
 *
 * @author theEvilReaper
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 1.0.0
 **/
public final class MapPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapPool.class);

    /**
     * The system property that names the world the lobby starts with.
     */
    public static final String LOBBY_MAP_PROPERTY = "TITAN_LOBBY_MAP";

    /**
     * The name of the world the pool falls back to when the named one is absent.
     */
    public static final String DEFAULT_MAP_NAME = "world";

    private final String requestedMapName;
    private boolean requestedMapSelected;
    private List<MapEntry> referenceList;
    private MapEntry selectedMap;
    private final Function<Stream<Path>, List<MapEntry>> filterMaps;

    /**
     * Creates a new instance of the map pool. It will load all maps from the given
     * path.
     *
     * @param path       the path where the maps are stored
     * @param filterMaps the filter that decides which directories count as a world
     */
    public MapPool(Path path, Function<Stream<Path>, List<MapEntry>> filterMaps) {
        this.filterMaps = filterMaps;
        // Read per instance rather than once per class: a static field would freeze the value at
        // class load, which is both untestable and a source of surprises when the property is set
        // from code rather than the command line.
        this.requestedMapName = System.getProperty(LOBBY_MAP_PROPERTY, DEFAULT_MAP_NAME);
        this.referenceList = loadMapsEntries(path);
        this.peekMap();
    }

    /**
     * Selects the world the lobby starts with.
     * <p>
     * The property wins whenever the world it names exists. When it does not, the searched name and
     * the found names are logged together — the two halves that make a typo obvious — and the
     * default world is used instead. Only a pool without any world at all is fatal, because there
     * is
     * then nothing left to start with.
     * </p>
     */
    private void peekMap() {
        Check.argCondition(this.referenceList.isEmpty(), "The map list is empty");

        Optional<MapEntry> requested = findMap(this.requestedMapName);
        if (requested.isPresent()) {
            this.selectedMap = requested.get();
            this.requestedMapSelected = true;
            return;
        }

        LOGGER.warn(describeMissingMap(this.requestedMapName, availableMapNames()));

        Optional<MapEntry> fallback = findMap(DEFAULT_MAP_NAME);
        if (fallback.isPresent()) {
            this.selectedMap = fallback.get();
            return;
        }

        // Neither the named world nor the default one is there. Refusing to start would leave the
        // lobby down over a naming question, so the first world that was found is used and the
        // situation is reported loudly enough to be fixed.
        this.selectedMap = this.referenceList.getFirst();
        LOGGER.warn("The default world '{}' does not exist either. Falling back to '{}'.", DEFAULT_MAP_NAME, this.selectedMap.path().getFileName().toString());
    }

    /**
     * Builds the warning that reports a world which the property named but which is not there.
     * <p>
     * The message is built rather than formatted into the log call so that the rule behind it —
     * both the searched name and the found ones have to appear — is one a test can hold the code
     * to.
     * </p>
     *
     * @param requested the name of the world that was searched for
     * @param found     the names of the worlds that are present
     * @return the warning to log
     */
    static String describeMissingMap(String requested, List<String> found) {
        return "The world '" + requested + "' named by the system property " + LOBBY_MAP_PROPERTY + " does not exist. Found worlds: " + String.join(", ", found) + ". Falling back to the default world '" + DEFAULT_MAP_NAME + "'.";
    }

    /**
     * Looks up a world by the name of its directory, ignoring case.
     *
     * @param name the name of the world directory
     * @return the entry of that world, or empty when no world carries the name
     */
    private Optional<MapEntry> findMap(String name) {
        return this.referenceList.stream().filter(mapEntry -> mapEntry.path().getFileName().toString().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Gets the names of every world the pool found, for a log line that has to name them.
     *
     * @return the names of the found worlds
     */
    List<String> availableMapNames() {
        return this.referenceList.stream().map(mapEntry -> mapEntry.path().getFileName().toString()).collect(Collectors.toList());
    }

    /**
     * Loads all maps from the given path. It will filter all directories and create
     * a new {@link MapEntry} instance.
     *
     * @param path
     *             the path where the maps are stored
     * @return a list with all available maps
     */
    private List<MapEntry> loadMapsEntries(Path path) {
        List<MapEntry> mapEntries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(path)) {
            mapEntries = this.filterMaps.apply(stream.filter(Files::isDirectory));
        } catch (IOException exception) {
            MinecraftServer.getExceptionManager().handleException(exception);
            LOGGER.error("Unable to load maps from path {}", path, exception);
        }
        return mapEntries;
    }

    /**
     * Gets the name of the world the system property asked for, which is
     * {@value #DEFAULT_MAP_NAME} when the property is not set.
     *
     * @return the name of the requested world
     */
    public String getRequestedMapName() {
        return this.requestedMapName;
    }

    /**
     * Answers whether the world the property named is the one that was selected.
     * <p>
     * A false here is the fallback case: the named world was not found and the pool went on with
     * another one rather than refusing to start.
     * </p>
     *
     * @return true if the requested world was found, otherwise false
     */
    public boolean isRequestedMapSelected() {
        return this.requestedMapSelected;
    }

    /**
     * Gets the selected map entry.
     *
     * @return the selected map entry
     */
    public MapEntry getMapEntry() {
        return this.selectedMap;
    }

    /**
     * Removes the selected map from the list. If the list is empty it will throw an
     * exception.
     */
    public void clear() {
        this.referenceList.clear();
        this.referenceList = null;
    }

    /**
     * Gets all available maps from the pool.
     *
     * @return an unmodifiable list with all available maps
     */
    public @UnmodifiableView List<MapEntry> getAvailableMaps() {
        return Collections.unmodifiableList(this.referenceList);
    }
}
