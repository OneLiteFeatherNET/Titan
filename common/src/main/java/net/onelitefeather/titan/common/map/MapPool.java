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
package net.onelitefeather.titan.common.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The map pool is responsible for managing the available maps. It will load all
 * maps data from the given path and store them. It would not load the map
 * itself over a {@link net.minestom.server.instance.anvil.AnvilLoader}
 * instance. This behavior is handled by another class.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class MapPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(MapPool.class);
	private static final String LOBBY_MAP_NAME = System.getProperty("TITAN_LOBBY_MAP", "world");

	private List<MapEntry> referenceList;
	private MapEntry selectedMap;
	private final Function<Stream<Path>, List<MapEntry>> filterMaps;

	/**
	 * Creates a new instance of the map pool. It will load all maps from the given
	 * path.
	 *
	 * @param path
	 *            the path where the maps are stored
	 */
	public MapPool(@NotNull Path path, @NotNull Function<Stream<Path>, List<MapEntry>> filterMaps) {
		this.filterMaps = filterMaps;
		this.referenceList = loadMapsEntries(path);
		this.peekMap();
	}

	private void peekMap() {
		Check.argCondition(this.referenceList.isEmpty(), "The map list is empty");
		if (this.referenceList.size() == 1) {
			this.selectedMap = this.referenceList.getFirst();
			return;
		}
		this.selectedMap = this.referenceList.stream()
				.filter(mapEntry -> mapEntry.path().getFileName().toString().equalsIgnoreCase(LOBBY_MAP_NAME))
				.findFirst().orElseThrow();
	}

	/**
	 * Loads all maps from the given path. It will filter all directories and create
	 * a new {@link MapEntry} instance.
	 *
	 * @param path
	 *            the path where the maps are stored
	 * @return a list with all available maps
	 */
	private @NotNull List<MapEntry> loadMapsEntries(@NotNull Path path) {
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
	 * Gets the selected map entry.
	 *
	 * @return the selected map entry
	 */
	public @NotNull MapEntry getMapEntry() {
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
	public @NotNull @UnmodifiableView List<MapEntry> getAvailableMaps() {
		return Collections.unmodifiableList(this.referenceList);
	}
}
