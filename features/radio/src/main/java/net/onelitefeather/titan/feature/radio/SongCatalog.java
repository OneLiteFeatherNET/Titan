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
package net.onelitefeather.titan.feature.radio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minestom.server.item.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Reads {@code songs.yaml} and the MIDI files it names, once at start. A broken entry or file is
 * logged with its name and skipped; nothing in here stops the lobby.
 */
final class SongCatalog {

    static final String INDEX = "songs.yaml";

    private static final Logger LOG = LoggerFactory.getLogger(SongCatalog.class);
    private static final String DISC_PREFIX = "MUSIC_DISC_";
    private static final Map<String, Material> DISCS = discsByName();

    private SongCatalog() {
    }

    /** One entry that was not loaded, with the file or entry it concerns and the reason. */
    record Rejection(String name, String reason) {
    }

    /** The songs in configuration order and the entries that were rejected. */
    record Load(List<MidiSong> songs, List<Rejection> rejections) {

        static Load empty() {
            return new Load(List.of(), List.of());
        }
    }

    /** Loads the songs of {@code directory}; a missing directory or index means no songs. */
    static Load load(Path directory) {
        Path index = directory.resolve(INDEX);
        if (!Files.isDirectory(directory) || !Files.isRegularFile(index)) {
            return Load.empty();
        }
        List<?> entries;
        try {
            entries = entries(index);
        } catch (IOException | RuntimeException e) {
            return unreadableIndex();
        }
        Builder builder = new Builder(directory);
        for (Object entry : entries) {
            builder.add(entry);
        }
        return builder.build();
    }

    private static Load unreadableIndex() {
        String reason = "it is not a readable songs list";
        LOG.warn("Radio song {} skipped: {}", INDEX, reason);
        return new Load(List.of(), List.of(new Rejection(INDEX, reason)));
    }

    private static List<?> entries(Path index) throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object root = yaml.load(Files.readString(index));
        if (root == null) {
            return List.of();
        }
        if (!(root instanceof Map<?, ?> map) || !(map.get("songs") instanceof List<?> songs)) {
            throw new IOException("the file needs a songs list");
        }
        return songs;
    }

    private static Map<String, Material> discsByName() {
        Map<String, Material> discs = new HashMap<>();
        for (Material material : Material.values()) {
            String name = material.key().value().toUpperCase(Locale.ROOT);
            if (name.startsWith(DISC_PREFIX)) {
                discs.put(name, material);
            }
        }
        return Map.copyOf(discs);
    }

    /** Collects the songs of one file in order, rejecting what does not fit. */
    private static final class Builder {

        private final Path directory;
        private final List<MidiSong> songs = new ArrayList<>();
        private final List<Rejection> rejections = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();
        private final Set<Material> discs = new HashSet<>();

        Builder(Path directory) {
            this.directory = directory;
        }

        void add(Object entry) {
            if (!(entry instanceof Map<?, ?> fields)) {
                reject("songs.yaml", "an entry is not a mapping");
                return;
            }
            String id = text(fields, "id");
            String file = text(fields, "file");
            String name = file != null ? file : String.valueOf(id);
            if (id == null || file == null || text(fields, "title") == null || text(fields, "artist") == null) {
                reject(name, "an entry needs id, title, artist and file");
                return;
            }
            String discName = text(fields, "disc");
            Material disc = discName == null ? null : DISCS.get(discName.toUpperCase(Locale.ROOT));
            if (disc == null) {
                reject(name, "disc must be a music disc, was " + discName);
                return;
            }
            if (this.ids.contains(id)) {
                reject(name, "the id " + id + " is already used");
                return;
            }
            if (this.discs.contains(disc)) {
                reject(name, "the disc " + disc.key().value() + " is already used by another song");
                return;
            }
            if (this.songs.size() == RadioLimits.MAX_SONGS) {
                reject(name, "only " + RadioLimits.MAX_SONGS + " songs are loaded");
                return;
            }
            loadFile(name, id, text(fields, "title"), text(fields, "artist"), disc, file);
        }

        // The id and the disc are reserved only once the song is accepted, so a rejected entry blocks nothing.
        private void loadFile(String name, String id, String title, String artist, Material disc, String file) {
            Path path = this.directory.resolve(file).normalize();
            if (!path.startsWith(this.directory.normalize()) || !Files.isRegularFile(path)) {
                reject(name, "the file is missing or outside radio/");
                return;
            }
            try {
                if (Files.size(path) > RadioLimits.MAX_FILE_BYTES) {
                    reject(name, "the file is larger than 512 KiB");
                    return;
                }
                MidiSong song = MidiSongParser.parse(id, title, artist, disc, RadioCategories.OTHER, Files.readAllBytes(path));
                this.ids.add(id);
                this.discs.add(disc);
                this.songs.add(song);
                LOG.info("Radio song {} loaded", name);
            } catch (SongFormatException e) {
                reject(name, e.getMessage());
            } catch (IOException | RuntimeException e) {
                reject(name, "the file cannot be read");
            }
        }

        private void reject(String name, String reason) {
            LOG.warn("Radio song {} skipped: {}", name, reason);
            this.rejections.add(new Rejection(name, reason));
        }

        Load build() {
            return new Load(List.copyOf(this.songs), List.copyOf(this.rejections));
        }

        private static String text(Map<?, ?> fields, String key) {
            Object value = fields.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                return null;
            }
            return String.valueOf(value).trim();
        }
    }
}
