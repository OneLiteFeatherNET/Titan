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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runs the one-time switch from the lobby's original {@code app.json} to {@code application.yaml}
 * (see {@code design.md}, decision 4 of the {@code standardized-config-profiles} change, and the
 * {@code lobby-module-config} spec requirement "Bestehende app.json wird einmalig umgestellt").
 * <p>
 * {@link #migrate(Path)} runs in the bootstrap phase, before the {@code avaje-config}
 * {@code Configuration} is built, so that {@code application.yaml} exists by the time the regular
 * configuration loading pipeline runs:
 * <ul>
 * <li>{@code app.json} present, no {@code application.yaml}: the document is read (a flat v1
 * document is mapped to sections by {@link LegacyConfigMigration#toSectioned(JsonObject, String)},
 * a v2 document is taken as-is), written to {@code application.yaml}, and {@code app.json} is
 * renamed to {@code app.json.migrated}. A WARN names the switch and the dropped legacy keys, if
 * any.</li>
 * <li>Both files present: {@code app.json} is left untouched, and a WARN says it is ignored.</li>
 * <li>Neither file present: nothing happens.</li>
 * <li>{@code app.json} is not valid JSON: {@link ConfigException} names the file and the parser's
 * position; nothing is renamed or written.</li>
 * </ul>
 * The YAML is written first, to a sibling temporary file that is then moved into place, and
 * {@code app.json} is renamed only afterwards - so a crash mid-migration can never leave a
 * half-written {@code application.yaml} next to an already-renamed {@code app.json}.
 * <p>
 * Before writing, {@link #convertNavigatorEntriesToMap(JsonObject)} turns a {@code
 * navigator.entries} JSON list - the only shape {@code app.json} ever had - into a map keyed by a
 * name derived from each entry's {@code displayName}, matching decision 3's
 * {@code NavigatorConfig(String, Map<String, Entry>)} shape. See that method's Javadoc for the
 * naming rule.
 */
public final class AppJsonMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppJsonMigration.class);

    private static final String APP_JSON = "app.json";
    private static final String APPLICATION_YAML = "application.yaml";
    private static final String MIGRATED_SUFFIX = ".migrated";

    /**
     * Switches {@code app.json} in {@code workingDir} to {@code application.yaml}, if there is
     * anything to switch. See the class documentation for the exact behaviour in every case.
     *
     * @param workingDir the directory the lobby reads its configuration from
     * @throws ConfigException if {@code app.json} exists but is not valid JSON
     */
    public void migrate(Path workingDir) {
        Path appJson = workingDir.resolve(APP_JSON);
        if (!Files.exists(appJson)) {
            return;
        }

        Path applicationYaml = workingDir.resolve(APPLICATION_YAML);
        if (Files.exists(applicationYaml)) {
            LOGGER.warn("{} exists together with {} - {} is ignored, only {} is read", APP_JSON, APPLICATION_YAML, APP_JSON, APPLICATION_YAML);
            return;
        }

        String fileName = fileNameOf(appJson);
        JsonObject root = readJsonObject(appJson, fileName);

        JsonObject sectioned = root;
        if (LegacyConfigMigration.isLegacy(root)) {
            sectioned = LegacyConfigMigration.toSectioned(root, fileName);
            LegacyConfigMigration.logDroppedKeys(appJson, root);
        }
        // configVersion was only ever a marker so isLegacy(JsonObject) above could tell a v1 from
        // a v2 app.json; it is a leftover of the old JSON format with no meaning in
        // application.yaml, so it never survives into the migrated file - neither the v1 path
        // (LegacyConfigMigration#toSectioned never adds it) nor here, where a v2 document is
        // otherwise taken as-is.
        sectioned.remove("configVersion");
        convertNavigatorEntriesToMap(sectioned);

        writeYaml(applicationYaml, sectioned);

        Path migratedAppJson = workingDir.resolve(APP_JSON + MIGRATED_SUFFIX);
        renameToMigrated(appJson, migratedAppJson);

        LOGGER.warn("Migrated {} to {} - the original file was renamed to {}; to roll back, rename it back to {} before starting an older build", fileName, APPLICATION_YAML, migratedAppJson.getFileName(), APP_JSON);
    }

    /**
     * Converts {@code navigator.entries} from a JSON list (the only shape {@code app.json} ever
     * had) into a map keyed by a name derived from each entry's {@code displayName}, matching the
     * {@code NavigatorConfig(String, Map<String, Entry>)} shape decision 3 of {@code design.md}
     * chose. MiniMessage tags are stripped, the remainder is lower-cased and only {@code [a-z0-9]}
     * is kept; an empty or already-taken name falls back to {@code slot<N>}/{@code name-<N>}, where
     * {@code N} is the entry's {@code slot}. Does nothing if {@code navigator} or
     * {@code navigator.entries} is absent, or if {@code entries} is already a map - e.g. someone
     * hand-wrote a v2 {@code app.json} in the new shape already.
     */
    private static void convertNavigatorEntriesToMap(JsonObject sectioned) {
        if (!sectioned.has("navigator") || !sectioned.get("navigator").isJsonObject()) {
            return;
        }
        JsonObject navigator = sectioned.getAsJsonObject("navigator");
        if (!navigator.has("entries") || !navigator.get("entries").isJsonArray()) {
            return;
        }
        JsonArray entries = navigator.getAsJsonArray("entries");
        JsonObject entriesByName = new JsonObject();
        Set<String> usedNames = new HashSet<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String name = deriveEntryName(entry, usedNames);
            usedNames.add(name);
            entriesByName.add(name, entry);
        }
        navigator.add("entries", entriesByName);
    }

    /**
     * Derives a navigator entry's map key from its {@code displayName}: strips MiniMessage tags
     * ({@code <...>}), lower-cases the rest, and keeps only {@code [a-z0-9]}. Falls back to
     * {@code slot<N>} if that leaves nothing, or appends {@code -<N>} if the name is already used
     * by an earlier entry in the same document - {@code N} being this entry's {@code slot}.
     */
    private static String deriveEntryName(JsonObject entry, Set<String> usedNames) {
        String slot = entry.has("slot") ? entry.get("slot").getAsString() : "";
        String displayName = entry.has("displayName") ? entry.get("displayName").getAsString() : "";
        String stripped = displayName.replaceAll("<[^>]*>", "");
        String candidate = stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (candidate.isEmpty()) {
            return "slot" + slot;
        }
        if (usedNames.contains(candidate)) {
            return candidate + "-" + slot;
        }
        return candidate;
    }

    private JsonObject readJsonObject(Path file, String fileName) {
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
        return parsed.getAsJsonObject();
    }

    private static String describe(JsonParseException e) {
        Throwable root = e.getCause() != null ? e.getCause() : e;
        String message = root.getMessage();
        return message != null ? message : root.getClass().getSimpleName();
    }

    private static void renameToMigrated(Path appJson, Path migratedAppJson) {
        try {
            Files.move(appJson, migratedAppJson, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to rename " + appJson + " to " + migratedAppJson, e);
        }
    }

    private static String fileNameOf(Path file) {
        return file.getFileName() != null ? file.getFileName().toString() : file.toString();
    }

    /**
     * Writes {@code document} to {@code target} as block-style YAML, first to a sibling temporary
     * file that is then moved into place with {@link StandardCopyOption#ATOMIC_MOVE} (falling back
     * to a plain {@link StandardCopyOption#REPLACE_EXISTING} move only if the file system does not
     * support an atomic move), so a reader never observes a half-written file and a failure while
     * writing never corrupts a previous, still-valid {@code application.yaml}.
     */
    private static void writeYaml(Path target, JsonObject document) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        Object plain = toPlainObject(document);

        Path parent = target.getParent();
        Path tempFile = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            tempFile = Files.createTempFile(parent, target.getFileName() + ".", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                yaml.dump(plain, writer);
            }
            moveIntoPlace(tempFile, target);
            tempFile = null;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write config file " + target, e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // best-effort cleanup; the target file was never touched
                }
            }
        }
    }

    private static void moveIntoPlace(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Converts a Gson {@link JsonElement} tree into plain {@link Map}, {@link List} and boxed
     * primitive values, the shapes SnakeYAML's {@link Yaml#dump(Object)} knows how to render in
     * block style. Object key order is preserved ({@link JsonObject} itself preserves insertion
     * order, and this uses a {@link LinkedHashMap} to keep it), which is what makes the migrated
     * {@code application.yaml} read in the same order as the original {@code app.json}.
     */
    private static Object toPlainObject(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), toPlainObject(entry.getValue()));
            }
            return map;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> list = new ArrayList<>(array.size());
            for (JsonElement item : array) {
                list.add(toPlainObject(item));
            }
            return list;
        }
        return toPlainValue(element.getAsJsonPrimitive());
    }

    private static Object toPlainValue(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            String asString = primitive.getAsString();
            boolean isDecimal = asString.indexOf('.') >= 0 || asString.indexOf('e') >= 0 || asString.indexOf('E') >= 0;
            return isDecimal ? primitive.getAsDouble() : (Object) primitive.getAsLong();
        }
        return primitive.getAsString();
    }
}
