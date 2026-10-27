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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minestom.server.item.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Reads {@code songs.yaml} and the categories it declares, once at start. Only the metadata and the
 * MIDI header of each file are checked here; the notes are read when a title starts. A broken entry
 * is logged with its name and skipped; nothing in here stops the lobby.
 */
final class SongCatalog {

    static final String INDEX = "songs.yaml";

    private static final Logger LOG = LoggerFactory.getLogger(SongCatalog.class);
    private static final String DISC_PREFIX = "MUSIC_DISC_";
    private static final Map<String, Material> DISCS = discsByName();
    private static final Map<String, Material> MATERIALS = materialsByName();
    private static final byte[] MIDI_HEADER = "MThd".getBytes(StandardCharsets.US_ASCII);
    private static final Material FALLBACK_ICON = Material.PAPER;

    private SongCatalog() {
    }

    /** One entry that was not loaded, with the file or entry it concerns and the reason. */
    record Rejection(String name, String reason) {
    }

    /**
     * The songs in configuration order, the declared categories in declaration order, and the
     * entries that were rejected.
     */
    record Load(List<MidiSong> songs, List<RadioCategory> categories, List<Rejection> rejections) {

        static Load empty() {
            return new Load(List.of(), List.of(), List.of());
        }
    }

    /** Loads the songs of {@code directory}; a missing directory or index means no songs. */
    static Load load(Path directory) {
        Path index = directory.resolve(INDEX);
        if (!Files.isDirectory(directory) || !Files.isRegularFile(index)) {
            return Load.empty();
        }
        Map<?, ?> root;
        try {
            root = root(index);
        } catch (IOException | RuntimeException e) {
            return unreadableIndex();
        }
        Builder builder = new Builder(directory);
        builder.declareCategories(root.get("categories"));
        for (Object entry : songs(root)) {
            builder.add(entry);
        }
        return builder.build();
    }

    private static Load unreadableIndex() {
        String reason = "it is not a readable songs list";
        LOG.warn("Radio song {} skipped: {}", INDEX, reason);
        return new Load(List.of(), List.of(), List.of(new Rejection(INDEX, reason)));
    }

    private static Map<?, ?> root(Path index) throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object root = yaml.load(Files.readString(index));
        if (root == null) {
            return Map.of();
        }
        if (!(root instanceof Map<?, ?> map)) {
            throw new IOException("the file needs a mapping with songs and categories");
        }
        if (map.get("songs") != null && !(map.get("songs") instanceof List<?>)) {
            throw new IOException("songs must be a list");
        }
        return map;
    }

    // A file with categories only is valid: it declares categories and no songs yet.
    private static List<?> songs(Map<?, ?> root) {
        return root.get("songs") instanceof List<?> songs ? songs : List.of();
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

    private static Map<String, Material> materialsByName() {
        Map<String, Material> materials = new HashMap<>();
        for (Material material : Material.values()) {
            materials.put(material.key().value().toUpperCase(Locale.ROOT), material);
        }
        return Map.copyOf(materials);
    }

    /** Collects the categories and the songs of one file in order, rejecting what does not fit. */
    private static final class Builder {

        private final Path directory;
        private final Map<String, RadioCategory> categories = new LinkedHashMap<>();
        private final List<MidiSong> songs = new ArrayList<>();
        private final List<Rejection> rejections = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();

        Builder(Path directory) {
            this.directory = Objects.requireNonNull(directory, "directory must not be null");
        }

        void declareCategories(Object raw) {
            if (raw == null) {
                return;
            }
            if (!(raw instanceof List<?> entries)) {
                reject(INDEX, "categories must be a list");
                return;
            }
            for (Object entry : entries) {
                declareCategory(entry);
            }
        }

        private void declareCategory(Object entry) {
            if (!(entry instanceof Map<?, ?> fields)) {
                reject(INDEX, "a category is not a mapping");
                return;
            }
            String id = text(fields, "id");
            String name = "category " + (id == null ? "without id" : id);
            if (!RadioCategory.isValidId(id)) {
                reject(name, "the id must be lower case letters, digits, underscore or dash, up to 32 characters");
                return;
            }
            if (RadioCategory.isReserved(id)) {
                reject(name, "the id " + id + " is reserved for a built-in category");
                return;
            }
            if (this.categories.containsKey(id)) {
                reject(name, "the id " + id + " is already declared");
                return;
            }
            if (this.categories.size() >= RadioCategories.MAX_DECLARED) {
                reject(name, "more than " + RadioCategories.MAX_DECLARED + " categories are declared");
                return;
            }
            String[] names = categoryNames(id, fields.get("name"));
            Material icon = categoryIcon(id, text(fields, "icon"));
            this.categories.put(id, new RadioCategory(id, names[0], names[1], icon, categoryOrder(id, fields.get("order"))));
        }

        // A name is text for both languages or a map with de and en; anything else, or nothing, takes the id.
        private static String[] categoryNames(String id, Object raw) {
            String german = null;
            String english = null;
            if (raw instanceof Map<?, ?> names) {
                german = text(names, "de");
                english = text(names, "en");
            } else if (raw instanceof String text && !text.isBlank()) {
                german = text.trim();
                english = german;
            }
            if (german == null && english == null) {
                LOG.warn("Radio category {} has no name, using its id", id);
                return new String[]{id, id};
            }
            return new String[]{german != null ? german : english, english != null ? english : german};
        }

        private static Material categoryIcon(String id, String name) {
            Material icon = name == null ? null : MATERIALS.get(name.toUpperCase(Locale.ROOT));
            if (icon == null) {
                LOG.warn("Radio category {} has no known icon ({}), using {}", id, name, FALLBACK_ICON);
                return FALLBACK_ICON;
            }
            return icon;
        }

        private static int categoryOrder(String id, Object raw) {
            if (raw == null) {
                return 0;
            }
            try {
                return Integer.parseInt(String.valueOf(raw).trim());
            } catch (NumberFormatException e) {
                LOG.warn("Radio category {} has an order that is no whole number ({}), using 0", id, raw);
                return 0;
            }
        }

        void add(Object entry) {
            if (!(entry instanceof Map<?, ?> fields)) {
                reject(INDEX, "an entry is not a mapping");
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
            loadFile(name, id, text(fields, "title"), text(fields, "artist"), disc, file, fields);
        }

        // The id is reserved only once the song is accepted, so a rejected entry blocks nothing.
        private void loadFile(String name, String id, String title, String artist, Material disc, String file, Map<?, ?> fields) {
            Path path = this.directory.resolve(file).normalize();
            if (!path.startsWith(this.directory.normalize()) || !Files.isRegularFile(path)) {
                reject(name, "the file is missing or outside radio/");
                return;
            }
            try {
                if (!hasMidiHeader(path)) {
                    reject(name, "the file is not a MIDI file");
                    return;
                }
            } catch (IOException | RuntimeException e) {
                reject(name, "the file cannot be read");
                return;
            }
            this.ids.add(id);
            this.songs.add(new MidiSong(id, title, artist, disc, songCategory(name, fields), path, text(fields, "midi")));
            LOG.info("Radio song {} loaded", name);
        }

        // Reads only the first four bytes: the body is read when the title plays.
        private static boolean hasMidiHeader(Path path) throws IOException {
            try (InputStream in = Files.newInputStream(path)) {
                return Arrays.equals(in.readNBytes(MIDI_HEADER.length), MIDI_HEADER);
            }
        }

        private RadioCategory songCategory(String name, Map<?, ?> fields) {
            String categoryId = text(fields, "category");
            if (categoryId == null) {
                LOG.warn("Radio song {} has no category, using other", name);
                return RadioCategories.OTHER;
            }
            RadioCategory declared = this.categories.get(categoryId);
            if (declared == null) {
                LOG.warn("Radio song {} names the category {}, which is not declared, using other", name, categoryId);
                return RadioCategories.OTHER;
            }
            return declared;
        }

        private void reject(String name, String reason) {
            LOG.warn("Radio song {} skipped: {}", name, reason);
            this.rejections.add(new Rejection(name, reason));
        }

        Load build() {
            return new Load(List.copyOf(this.songs), List.copyOf(this.categories.values()), List.copyOf(this.rejections));
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
