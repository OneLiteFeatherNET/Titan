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
package net.onelitefeather.titan.common.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

/**
 * Reads optional parts of a {@code map.json} entry: a missing or mistyped field is {@code null}
 * instead of an exception, so the core validator can name it and skip just that entry.
 */
final class LenientJson {

    private LenientJson() {
    }

    static @Nullable JsonObject object(@Nullable JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    static @Nullable String string(@Nullable JsonObject object, String name) {
        JsonElement value = object == null ? null : object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }

    static @Nullable Double number(@Nullable JsonObject object, String name) {
        JsonElement value = object == null ? null : object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsDouble() : null;
    }

    static boolean bool(@Nullable JsonObject object, String name) {
        JsonElement value = object == null ? null : object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }

    /** A position with x, y and z; {@code null} if one of them is missing. */
    static @Nullable Vec vec(@Nullable JsonElement element) {
        JsonObject object = object(element);
        Double x = number(object, "x");
        Double y = number(object, "y");
        Double z = number(object, "z");
        return x == null || y == null || z == null ? null : new Vec(x, y, z);
    }

    /** A position with its facing; yaw and pitch default to 0. */
    static @Nullable Pos pos(@Nullable JsonElement element) {
        Vec vec = vec(element);
        if (vec == null) {
            return null;
        }
        JsonObject object = object(element);
        Double yaw = number(object, "yaw");
        Double pitch = number(object, "pitch");
        return new Pos(vec.x(), vec.y(), vec.z(), yaw == null ? 0f : yaw.floatValue(), pitch == null ? 0f : pitch.floatValue());
    }

    /** The elements of an array; empty if the element is missing or is no array. */
    static List<JsonElement> elements(@Nullable JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<JsonElement> elements = new ArrayList<>(array.size());
        array.forEach(elements::add);
        return elements;
    }

    static int integer(@Nullable JsonObject object, String name) {
        Double value = number(object, name);
        return value == null ? 0 : value.intValue();
    }

    static double decimal(@Nullable JsonObject object, String name) {
        Double value = number(object, name);
        return value == null ? 0 : value;
    }

    static JsonObject point(Point point) {
        JsonObject object = new JsonObject();
        object.addProperty("x", point.x());
        object.addProperty("y", point.y());
        object.addProperty("z", point.z());
        return object;
    }
}
