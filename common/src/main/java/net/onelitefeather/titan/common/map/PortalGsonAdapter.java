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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalShape;

import java.lang.reflect.Type;

/**
 * Reads and writes the {@code portals} entries of a {@code map.json} (see the lobby-portals
 * contract): the {@code shape.type} discriminator picks {@code box} or {@code disc}. Unknown
 * fields are ignored, an unknown type or a missing geometry field is a {@link JsonParseException}.
 * A missing {@code id} or {@code task} is left to the {@code PortalValidator}, which reports it
 * together with every other problem.
 */
final class PortalGsonAdapter implements JsonDeserializer<Portal>, JsonSerializer<Portal> {

    private static final String BOX = "box";
    private static final String DISC = "disc";

    @Override
    public Portal deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = asObject(json, "portal");
        String id = string(object, "id");
        try {
            return new Portal(id, shape(object.get("shape")), string(object, "task"), string(object, "permission"));
        } catch (JsonParseException exception) {
            throw new JsonParseException("portal '" + id + "': " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            // getAsString/getAsDouble on an element of the wrong JSON type.
            throw new JsonParseException("portal '" + id + "': malformed value (" + exception.getMessage() + ")", exception);
        }
    }

    private static PortalShape shape(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new JsonParseException("missing 'shape'");
        }
        JsonObject shape = asObject(element, "'shape'");
        String kind = string(shape, "type");
        if (kind == null) {
            throw new JsonParseException("missing 'shape.type'");
        }
        return switch (kind) {
            case BOX -> new Box(point(shape, "min"), point(shape, "max"));
            case DISC ->
                new Disc(point(shape, "center"), number(shape, "radius", null), point(shape, "normal"));
            default -> throw new JsonParseException("unknown shape type '" + kind + "'");
        };
    }

    @Override
    public JsonElement serialize(Portal portal, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("id", portal.id());
        object.addProperty("task", portal.task());
        if (portal.permission() != null) {
            object.addProperty("permission", portal.permission());
        }
        object.add("shape", shape(portal.shape()));
        return object;
    }

    private static JsonObject shape(PortalShape shape) {
        JsonObject object = new JsonObject();
        switch (shape) {
            case Box box -> {
                object.addProperty("type", BOX);
                object.add("min", point(box.min()));
                object.add("max", point(box.max()));
            }
            case Disc disc -> {
                object.addProperty("type", DISC);
                object.add("center", point(disc.center()));
                object.addProperty("radius", disc.radius());
                object.add("normal", point(disc.normal()));
            }
        }
        return object;
    }

    private static JsonObject point(Vec vec) {
        JsonObject object = new JsonObject();
        object.addProperty("x", vec.x());
        object.addProperty("y", vec.y());
        object.addProperty("z", vec.z());
        return object;
    }

    private static Vec point(JsonObject parent, String name) {
        JsonObject point = asObject(parent.get(name), "'" + name + "'");
        return new Vec(number(point, "x", name), number(point, "y", name), number(point, "z", name));
    }

    private static double number(JsonObject object, String name, String parent) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            throw new JsonParseException("missing '" + (parent == null ? name : parent + "." + name) + "'");
        }
        return value.getAsDouble();
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static JsonObject asObject(JsonElement element, String what) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException("missing or malformed " + what);
        }
        return element.getAsJsonObject();
    }
}
