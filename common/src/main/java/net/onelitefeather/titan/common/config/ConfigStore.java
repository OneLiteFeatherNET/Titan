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
package net.onelitefeather.titan.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * A sectioned configuration store backed by a single JSON document, e.g. {@code app.json}.
 * <p>
 * Each module owns one named section of the document, keyed by its module id. A module asks for
 * its section with {@link #section(String, Class, Record)}, passing a default instance of its
 * config record; missing fields (including missing sections entirely) fall back to the matching
 * field of the default, never to {@code 0} or {@code null}. A record's compact constructor is the
 * place to validate its own values; it should throw {@link ConfigException#invalid(String, String)}
 * on a bad value, which this store completes with the section id before it escapes {@link
 * #section(String, Class, Record)}.
 * <p>
 * The document is kept as a single {@link JsonObject} in memory. {@link #save()} rewrites the
 * whole file with {@code configVersion} as the first key; sections that were not touched during
 * this run are written back unchanged. {@link #set(String, String, JsonElement)} and {@link
 * #setSection(String, Record)} change a single section without disturbing the rest of the
 * document, which is what lets the setup server change one value without losing another module's
 * settings (see {@code lobby-module-config} spec, "Changes from the setup server don't lose
 * values").
 * <p>
 * Opening a missing file starts an empty, in-memory document; opening a legacy flat document (no
 * {@code configVersion}, at least one recognized legacy key) runs {@link LegacyConfigMigration}.
 * In both cases {@link #flush()} performs a first save so the file on disk ends up with a section
 * for every module the caller asked for. A syntactically broken file is never touched: {@link
 * #open(Path)} throws before any {@link ConfigStore} instance exists.
 */
public final class ConfigStore {

    /**
     * The version written to {@code configVersion} by {@link #save()}.
     */
    public static final int CURRENT_CONFIG_VERSION = LegacyConfigMigration.TARGET_CONFIG_VERSION;

    private static final String CONFIG_VERSION_KEY = "configVersion";

    private final SectionBinder binder = new SectionBinder();

    private final Path file;
    private final String fileName;
    private JsonObject document;
    private boolean writeOnFlush;

    private ConfigStore(Path file, JsonObject document, boolean writeOnFlush) {
        this.file = file;
        this.fileName = file.getFileName() != null ? file.getFileName().toString() : file.toString();
        this.document = document;
        this.writeOnFlush = writeOnFlush;
    }

    /**
     * Opens the configuration document at {@code file}.
     * <ul>
     * <li>A missing file yields an empty, in-memory document, remembered as freshly
     * "created" so {@link #flush()} performs a first save.</li>
     * <li>A document without {@code configVersion} that has at least one recognized legacy
     * key is migrated by {@link LegacyConfigMigration}, and is likewise remembered so {@link
     * #flush()} writes the migrated result.</li>
     * <li>A syntactically broken document throws {@link ConfigException} naming the file and
     * the parser's line/column; the file is never touched.</li>
     * </ul>
     *
     * @param file the path to the configuration file
     * @return a {@link ConfigStore} for that file
     */
    public static ConfigStore open(Path file) {
        String fileName = file.getFileName() != null ? file.getFileName().toString() : file.toString();
        if (!Files.exists(file)) {
            return new ConfigStore(file, new JsonObject(), true);
        }

        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config file " + file, e);
        }

        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(content);
        } catch (JsonParseException e) {
            throw ConfigException.malformed(fileName, describe(e));
        }
        if (!parsed.isJsonObject()) {
            throw ConfigException.malformed(fileName, "the document root must be a JSON object");
        }

        JsonObject root = parsed.getAsJsonObject();
        if (LegacyConfigMigration.isLegacy(root)) {
            JsonObject migrated = LegacyConfigMigration.migrate(file, root);
            return new ConfigStore(file, migrated, true);
        }
        return new ConfigStore(file, root, false);
    }

    /**
     * Loads the section named {@code id}, deep-merging it over {@code defaults}: the file's value
     * wins for every field it sets (objects merge recursively, arrays and primitives replace the
     * default outright), and a missing section or field falls back to the matching part of
     * {@code defaults}.
     * <p>
     * The merged section is remembered so a later {@link #save()} or {@link #flush()} writes it
     * back, which is how a first start ends up with every requested module's defaults on disk.
     *
     * @param id       the section id, matching a module id
     * @param type     the config record type
     * @param defaults a fully populated default instance
     * @param <R>      the config record type
     * @return the section deserialized into {@code type}, with defaults applied for missing values
     * @throws ConfigException if the merged section fails the record's own validation, or if a
     *                         value in the file does not match its field's type (e.g. a string
     *                         where a {@code long} is expected)
     */
    public <R extends Record> R section(String id, Class<R> type, R defaults) {
        JsonElement existing = document.has(id) ? document.get(id) : null;
        SectionBinder.Bound<R> bound = binder.bind(fileName, id, type, defaults, existing);
        document.add(id, bound.merged());
        return bound.value();
    }

    /**
     * Sets a single field of a single section, leaving every other section and every other field
     * of this section untouched.
     *
     * @param section the section id
     * @param field   the field name within that section
     * @param value   the new value
     */
    public void set(String section, String field, JsonElement value) {
        JsonElement current = document.get(section);
        JsonObject sectionObject = (current != null && current.isJsonObject()) ? current.getAsJsonObject() : new JsonObject();
        sectionObject.add(field, value);
        document.add(section, sectionObject);
    }

    /**
     * Replaces a whole section with the serialized form of {@code value}, leaving every other
     * section untouched.
     *
     * @param id    the section id
     * @param value the record to store as that section
     */
    public void setSection(String id, Record value) {
        JsonElement element = binder.gson().toJsonTree(value, value.getClass());
        document.add(id, element);
    }

    /**
     * Writes the whole document to disk, pretty-printed, with {@code configVersion} as the first
     * key. Every other section is written back exactly as it currently is in memory.
     * <p>
     * The document is first written to a sibling temporary file and only then moved into place
     * with {@link StandardCopyOption#ATOMIC_MOVE} (falling back to a plain {@link
     * StandardCopyOption#REPLACE_EXISTING} move only if the file system does not support an atomic
     * move), so a reader never observes a half-written file and a failure while writing never
     * corrupts the previous, still-valid file.
     */
    public void save() {
        JsonObject output = new JsonObject();
        output.addProperty(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
        for (Map.Entry<String, JsonElement> entry : document.entrySet()) {
            if (CONFIG_VERSION_KEY.equals(entry.getKey())) {
                continue;
            }
            output.add(entry.getKey(), entry.getValue());
        }

        Path parent = file.getParent();
        Path directory = parent != null ? parent : Path.of("").toAbsolutePath();
        Path tempFile = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            tempFile = Files.createTempFile(directory, fileName + ".", ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                binder.gson().toJson(output, writer);
            }
            moveIntoPlace(tempFile, file);
            tempFile = null;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write config file " + file, e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // best-effort cleanup; the original file was never touched
                }
            }
        }

        this.document = output;
    }

    private static void moveIntoPlace(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Saves the document only if it was freshly created (missing file) or migrated from the
     * legacy format by {@link #open(Path)}. A start against an already up-to-date file is a
     * no-op, so requesting sections during startup does not rewrite an operator's file unless
     * something actually changed via {@link #set(String, String, JsonElement)} or {@link
     * #setSection(String, Record)}.
     */
    public void flush() {
        if (writeOnFlush) {
            save();
            writeOnFlush = false;
        }
    }

    private static String describe(JsonParseException e) {
        Throwable root = e.getCause() != null ? e.getCause() : e;
        String message = root.getMessage();
        return message != null ? message : root.getClass().getSimpleName();
    }
}
