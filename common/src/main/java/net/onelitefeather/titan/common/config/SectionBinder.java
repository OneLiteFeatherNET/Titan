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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The binding core shared by every source of a config section - today {@link ConfigStore} (a
 * single {@code app.json} document) and {@link ConfigSections} (flattened {@code avaje-config}
 * keys reassembled into a tree).
 * <p>
 * Given a section's raw JSON value (or {@code null} if the section is absent) and a default
 * instance of the module's config record, {@link #bind} deep-merges the two, deserializes the
 * result with Gson, unwraps a {@link ConfigException} thrown by the record's compact constructor,
 * turns a plain Gson type mismatch (e.g. a string where a {@code long} is expected) into a {@link
 * ConfigException} naming the offending field, and warns once about keys the record does not
 * declare.
 * <p>
 * Package-private: callers only reach this through {@link ConfigStore#section(String, Class,
 * Record)} or {@link ConfigSections#section(String, Class, Record)}.
 */
final class SectionBinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SectionBinder.class);

    private final Gson gson;

    SectionBinder() {
        this(createGson());
    }

    private SectionBinder(Gson gson) {
        this.gson = gson;
    }

    /**
     * The single {@link Gson} instance every section is bound with - shared with {@link
     * ConfigStore}, which also uses it to serialize a whole section back to JSON for {@link
     * ConfigStore#setSection(String, Record)} and {@link ConfigStore#save()}, so the same {@link
     * Vec}/{@link Pos}/{@link Key} adapters apply everywhere a config record is (de)serialized.
     */
    Gson gson() {
        return gson;
    }

    /**
     * The result of binding a section: the raw JSON, deep-merged over the defaults (what a caller
     * such as {@link ConfigStore} keeps around to write back unchanged), and the value
     * deserialized from it.
     */
    record Bound<R extends Record>(JsonElement merged, R value) {
    }

    /**
     * Binds section {@code id}, whose raw value (or {@code null} if the section is missing
     * entirely) is {@code existing}, deep-merged over {@code defaults}.
     *
     * @param fileName the name of the file the section was read from, used only to build the
     *                 exception/warning message; {@code null} when the section did not come from
     *                 a single named file (e.g. {@link ConfigSections}, which reads from
     *                 {@code application.yaml}, profiles and overrides at once)
     * @param id       the section id, matching a module id
     * @param type     the config record type
     * @param defaults a fully populated default instance
     * @param existing the section's raw value as read from its source, or {@code null} if the
     *                 section is missing
     * @param <R>      the config record type
     * @return the merged raw JSON together with the value deserialized into {@code type}
     * @throws ConfigException if the merged section fails the record's own validation, or if a
     *                         value does not match its field's type (e.g. a string where a
     *                         {@code long} is expected)
     */
    <R extends Record> Bound<R> bind(@Nullable String fileName, String id, Class<R> type, R defaults, @Nullable JsonElement existing) {
        JsonElement defaultElement = gson.toJsonTree(defaults, type);
        warnAboutUnknownKeys(fileName, id, type, existing);
        JsonElement merged = deepMerge(existing, defaultElement);

        R value;
        try {
            value = gson.fromJson(merged, type);
        } catch (RuntimeException e) {
            ConfigException configException = unwrap(e);
            if (configException != null) {
                throw configException.withSection(id).withFile(fileName);
            }
            throw typeMismatch(fileName, id, type, merged, e);
        }
        return new Bound<>(merged, value);
    }

    /**
     * Logs one warning naming every key {@code existing} has that {@code type}'s record components
     * do not declare - e.g. a leftover {@code elytra.boostMultiplier} in a section that used to
     * have it. Never changes {@code existing} itself: an unknown key is not this class's business
     * to drop or rewrite, only to flag.
     *
     * @param fileName the file name to name in the warning, or {@code null} to omit it
     * @param id       the section id, used only for the log line
     * @param type     the config record type the section is about to be deserialized into
     * @param existing the section's raw value, or {@code null} if the section is missing entirely
     */
    private void warnAboutUnknownKeys(@Nullable String fileName, String id, Class<? extends Record> type, @Nullable JsonElement existing) {
        if (existing == null || !existing.isJsonObject()) {
            return;
        }
        Set<String> declared = new HashSet<>();
        for (RecordComponent component : type.getRecordComponents()) {
            declared.add(component.getName());
        }
        List<String> unknown = new ArrayList<>();
        for (String key : existing.getAsJsonObject().keySet()) {
            if (!declared.contains(key)) {
                unknown.add(key);
            }
        }
        if (unknown.isEmpty()) {
            return;
        }
        if (fileName != null) {
            LOGGER.warn("{}: {} contains unknown keys {} - they are ignored", fileName, id, unknown);
        } else {
            LOGGER.warn("{} contains unknown keys {} - they are ignored", id, unknown);
        }
    }

    private static ConfigException unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConfigException configException) {
                return configException;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * Turns a raw Gson deserialization failure (a JSON value that does not match its record
     * component's type, e.g. a string where a {@code long} is expected) into a {@link
     * ConfigException} naming the file (if known) and the section. Gson itself does not attach a
     * field name to this kind of failure, so the merged section is checked field by field against
     * {@code type}'s own record components; the first component whose value cannot represent that
     * component's type is reported as the offending field. When no such field can be found, the
     * exception falls back to a section-level message built from the original failure.
     */
    private static ConfigException typeMismatch(@Nullable String fileName, String id, Class<? extends Record> type, JsonElement merged, RuntimeException cause) {
        RecordComponent mismatched = findMismatchedComponent(type, merged);
        if (mismatched != null) {
            JsonElement value = merged.getAsJsonObject().get(mismatched.getName());
            return ConfigException.invalid(mismatched.getName(), mismatchReason(mismatched.getType(), value)).withSection(id).withFile(fileName);
        }
        String detail = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        return ConfigException.malformed(fileName, "could not be read as '" + type.getSimpleName() + "': " + detail).withSection(id);
    }

    /**
     * Finds the first record component of {@code type} whose value in {@code merged} cannot
     * represent that component's declared type. Only simple, unambiguous mismatches (numeric and
     * boolean components) are detected; strings, nested records, collections and the custom {@link
     * Key}/{@link Vec}/{@link Pos} adapters are left to Gson's own error, which is reported as a
     * section-level message instead.
     */
    private static @Nullable RecordComponent findMismatchedComponent(Class<? extends Record> type, JsonElement merged) {
        if (!merged.isJsonObject()) {
            return null;
        }
        JsonObject object = merged.getAsJsonObject();
        for (RecordComponent component : type.getRecordComponents()) {
            JsonElement value = object.get(component.getName());
            if (value != null && isTypeMismatch(component.getType(), value)) {
                return component;
            }
        }
        return null;
    }

    private static boolean isTypeMismatch(Class<?> componentType, JsonElement value) {
        if (value.isJsonNull()) {
            return false;
        }
        if (isIntegerType(componentType)) {
            return !isParsableAsLong(value);
        }
        if (isDecimalType(componentType)) {
            return !isParsableAsDouble(value);
        }
        if (isBooleanType(componentType)) {
            return !isParsableAsBoolean(value);
        }
        return false;
    }

    private static boolean isIntegerType(Class<?> type) {
        return type == long.class || type == Long.class || type == int.class || type == Integer.class || type == short.class || type == Short.class || type == byte.class || type == Byte.class;
    }

    private static boolean isDecimalType(Class<?> type) {
        return type == double.class || type == Double.class || type == float.class || type == Float.class;
    }

    private static boolean isBooleanType(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    private static boolean isParsableAsLong(JsonElement value) {
        if (!value.isJsonPrimitive()) {
            return false;
        }
        try {
            Long.parseLong(value.getAsJsonPrimitive().getAsString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isParsableAsDouble(JsonElement value) {
        if (!value.isJsonPrimitive()) {
            return false;
        }
        try {
            Double.parseDouble(value.getAsJsonPrimitive().getAsString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isParsableAsBoolean(JsonElement value) {
        if (!value.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        return primitive.isBoolean() || "true".equalsIgnoreCase(primitive.getAsString()) || "false".equalsIgnoreCase(primitive.getAsString());
    }

    private static String mismatchReason(Class<?> componentType, JsonElement value) {
        String expected;
        if (isIntegerType(componentType)) {
            expected = "a whole number";
        } else if (isDecimalType(componentType)) {
            expected = "a number";
        } else if (isBooleanType(componentType)) {
            expected = "true or false";
        } else {
            expected = componentType.getSimpleName();
        }
        return "must be " + expected + ", was " + describeValue(value);
    }

    private static String describeValue(JsonElement value) {
        if (value.isJsonNull()) {
            return "null";
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return "\"" + value.getAsString() + "\"";
        }
        return value.toString();
    }

    /**
     * Deep-merges {@code overlay} (the value read from the source, possibly {@code null}) over
     * {@code base} (the serialized default): matching JSON objects merge key by key, recursively;
     * any other JSON type (array, primitive, {@code null}) is replaced outright by the overlay
     * when present.
     */
    private static JsonElement deepMerge(@Nullable JsonElement overlay, JsonElement base) {
        if (overlay == null || overlay.isJsonNull()) {
            return base;
        }
        if (base.isJsonObject() && overlay.isJsonObject()) {
            JsonObject baseObject = base.getAsJsonObject();
            JsonObject overlayObject = overlay.getAsJsonObject();
            JsonObject merged = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : baseObject.entrySet()) {
                String key = entry.getKey();
                merged.add(key, deepMerge(overlayObject.get(key), entry.getValue()));
            }
            for (Map.Entry<String, JsonElement> entry : overlayObject.entrySet()) {
                if (!merged.has(entry.getKey())) {
                    merged.add(entry.getKey(), entry.getValue());
                }
            }
            return merged;
        }
        return overlay;
    }

    private static Gson createGson() {
        PositionGsonAdapter positionAdapter = new PositionGsonAdapter();
        return new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Vec.class, positionAdapter).registerTypeAdapter(Pos.class, positionAdapter).registerTypeHierarchyAdapter(Key.class, new KeyGsonAdapter()).create();
    }
}
