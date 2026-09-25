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
import com.google.gson.JsonPrimitive;
import io.avaje.config.Configuration;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Binds a module's config record against an {@code avaje-config} {@link Configuration}: {@code
 * application.yaml}, its active profiles, an external file, environment variables and system
 * properties, in that rank order (see {@code lobby-module-config} spec, "Overrides have a fixed
 * rank order").
 * <p>
 * {@code avaje-config} only hands out flat, dotted keys (e.g. {@code sit.offset.x}, {@code
 * navigator.entries.survival.slot}) and comma-joined values for a YAML list of scalars (e.g.
 * {@code sit.allowedBlocks=stone,dirt,grass}) - see {@code design.md} decision 2's spike result.
 * {@link #section(String, Class, Record)} rebuilds a {@link JsonObject} tree from a section's flat
 * keys, splitting a value into a JSON array only where the target record component - found by
 * walking {@code type}'s own record components, recursing into nested records and into the value
 * type of a {@code Map<String, Record>} component - is actually a {@code List}/{@code Set}/array,
 * then delegates to the same, already-tested {@link SectionBinder} core {@link ConfigStore} uses.
 */
public final class ConfigSections {

    private final Configuration configuration;
    private final SectionBinder binder = new SectionBinder();

    public ConfigSections(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Loads the section named {@code id}, deep-merging it over {@code defaults}: a value the
     * configuration sets for a key under {@code id.} wins, and a missing section or single value
     * falls back to the matching part of {@code defaults}.
     *
     * @param id       the section id, matching a module id
     * @param type     the config record type
     * @param defaults a fully populated default instance
     * @param <R>      the config record type
     * @return the section deserialized into {@code type}, with defaults applied for missing values
     * @throws ConfigException if the merged section fails the record's own validation, or if a
     *                         value does not match its field's type (e.g. a non-numeric override
     *                         for a {@code long} field)
     */
    public <R extends Record> R section(String id, Class<R> type, R defaults) {
        Configuration scoped = configuration.forPath(id);
        Set<String> keys = scoped.keys();
        JsonElement existing = keys.isEmpty() ? null : buildTree(id, scoped, keys, type);
        return binder.bind(null, id, type, defaults, existing).value();
    }

    /**
     * Rebuilds a section's raw JSON value from its flat, dotted keys, e.g. {@code offset.x},
     * {@code offset.y}, {@code offset.z} become {@code {offset: {x: ..., y: ..., z: ...}}}.
     *
     * @throws ConfigException if two keys set a plain value and a nested value for the same path
     *                         at once (e.g. {@code offset} together with {@code offset.x}) - which
     *                         key would otherwise silently win depends only on the iteration order
     *                         of {@code keys}
     */
    private static JsonObject buildTree(String sectionId, Configuration scoped, Set<String> keys, Class<? extends Record> type) {
        JsonObject root = new JsonObject();
        for (String key : keys) {
            List<String> path = List.of(key.split("\\."));
            String value = scoped.get(key);
            addAtPath(root, sectionId, path, value, isListTyped(type, path));
        }
        return root;
    }

    /**
     * Adds {@code rawValue} at {@code path} within {@code root}, creating an intermediate {@link
     * JsonObject} for every path segment but the last, e.g. path {@code ["offset", "x"]} ends up
     * as {@code root.offset.x}.
     */
    private static void addAtPath(JsonObject root, String sectionId, List<String> path, String rawValue, boolean asList) {
        JsonObject current = root;
        for (int i = 0; i < path.size() - 1; i++) {
            String segment = path.get(i);
            JsonElement child = current.get(segment);
            JsonObject childObject;
            if (child == null) {
                childObject = new JsonObject();
                current.add(segment, childObject);
            } else if (child.isJsonObject()) {
                childObject = child.getAsJsonObject();
            } else {
                throw conflictingKey(sectionId, path.subList(0, i + 1));
            }
            current = childObject;
        }
        String leaf = path.get(path.size() - 1);
        if (current.has(leaf) && current.get(leaf).isJsonObject()) {
            throw conflictingKey(sectionId, path);
        }
        current.add(leaf, asList ? toJsonArray(rawValue) : new JsonPrimitive(rawValue));
    }

    /**
     * Reports that {@code fieldPath} (relative to {@code sectionId}) is set both as a plain value
     * and as a nested value at once, e.g. {@code sit.offset} together with {@code sit.offset.x}.
     */
    private static ConfigException conflictingKey(String sectionId, List<String> fieldPath) {
        String field = String.join(".", fieldPath);
        return ConfigException.invalid(field, "has both a plain value and nested fields set at the same time; remove one").withSection(sectionId);
    }

    private static JsonArray toJsonArray(String rawValue) {
        JsonArray array = new JsonArray();
        for (String element : rawValue.split(",", -1)) {
            array.add(element);
        }
        return array;
    }

    /**
     * Walks {@code rootType}'s own record components along {@code path} to decide whether the
     * value at that path belongs to a component actually typed {@code List}/{@code Set}/array -
     * the only case a comma-joined value must be split rather than left for Gson to coerce as a
     * single value. Descends into a nested record component, and - for a {@code Map<String,
     * Record>} component - consumes the dynamic map key that follows before continuing into the
     * map's value type, so a list nested inside a map value (e.g. a hypothetical
     * {@code entries.survival.tags}) is found too.
     */
    private static boolean isListTyped(Class<? extends Record> rootType, List<String> path) {
        Class<?> currentType = rootType;
        for (int i = 0; i < path.size(); i++) {
            if (!currentType.isRecord()) {
                return false;
            }
            RecordComponent component = findComponent(currentType, path.get(i));
            if (component == null) {
                return false;
            }
            if (i == path.size() - 1) {
                return isListLike(component.getType());
            }
            if (Map.class.isAssignableFrom(component.getType())) {
                Class<?> valueType = mapValueType(component);
                if (valueType == null || i + 2 > path.size()) {
                    return false;
                }
                currentType = valueType;
                i++; // consume the dynamic map key that follows the component name
                continue;
            }
            currentType = component.getType();
        }
        return false;
    }

    private static @Nullable RecordComponent findComponent(Class<?> type, String name) {
        for (RecordComponent component : type.getRecordComponents()) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return null;
    }

    private static boolean isListLike(Class<?> type) {
        return List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type) || type.isArray();
    }

    private static @Nullable Class<?> mapValueType(RecordComponent component) {
        Type genericType = component.getGenericType();
        if (genericType instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length == 2 && arguments[1] instanceof Class<?> valueClass) {
                return valueClass;
            }
        }
        return null;
    }
}
