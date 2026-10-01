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
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.PortalShape;

import java.lang.reflect.Type;

/**
 * Reads and writes the {@code portals} entries of a {@code map.json} (see the lobby-portals
 * contract): the {@code shape.type} discriminator picks {@code box} or {@code disc}. Unknown
 * fields are ignored, an unknown type or a missing geometry field is a {@link JsonParseException}.
 * A missing {@code id} or {@code task} is left to the {@code PortalValidator}, which reports it
 * together with every other problem. The optional {@code label} is read the same way: an unknown
 * {@code source.type} or {@code billboard} becomes a marker value and a missing {@code position}
 * stays {@code null}, all reported by the validator.
 */
final class PortalGsonAdapter implements JsonDeserializer<Portal>, JsonSerializer<Portal> {

    private static final String BOX = "box";
    private static final String DISC = "disc";
    private static final String LABEL = "label";
    private static final String TASK = "task";
    private static final String GROUP = "group";
    private static final String SERVICE = "service";
    private static final String LOCAL = "local";
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String PERMISSION = "permission";
    private static final String SHAPE = "shape";
    private static final String POSITION = "position";
    private static final String TEXT = "text";
    private static final String OFFLINE_TEXT = "offlineText";
    private static final String SOURCE = "source";
    private static final String BILLBOARD = "billboard";
    private static final String YAW = "yaw";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String CENTER = "center";
    private static final String RADIUS = "radius";
    private static final String NORMAL = "normal";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";

    @Override
    public Portal deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = asObject(json, "portal");
        String id = string(object, ID);
        try {
            return new Portal(id, shape(object.get(SHAPE)), string(object, TASK), string(object, PERMISSION), label(object.get(LABEL)));
        } catch (JsonParseException exception) {
            throw new JsonParseException("portal '" + id + "': " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            // getAsString/getAsDouble on an element of the wrong JSON type.
            throw new JsonParseException("portal '" + id + "': malformed value (" + exception.getMessage() + ")", exception);
        }
    }

    private static PortalLabel label(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        JsonObject label = asObject(element, "'label'");
        Vec position = isPresent(label, POSITION) ? point(label, POSITION) : null;
        String text = string(label, TEXT);
        String offlineText = string(label, OFFLINE_TEXT);
        LabelSource source = source(label.get(SOURCE));
        String billboardId = string(label, BILLBOARD);
        Billboard billboard = billboardId == null ? Billboard.CENTER : Billboard.byId(billboardId);
        float yaw = isPresent(label, YAW) ? (float) number(label, YAW, null) : 0f;
        return new PortalLabel(position, text, offlineText, source, billboard, yaw);
    }

    private static LabelSource source(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        JsonObject source = asObject(element, "'label.source'");
        String kind = string(source, TYPE);
        String name = string(source, NAME);
        if (kind == null) {
            return new LabelSource.Unknown(null);
        }
        return switch (kind) {
            case TASK -> new LabelSource.Task(name);
            case GROUP -> new LabelSource.Group(name);
            case SERVICE -> new LabelSource.Service(name);
            case LOCAL -> new LabelSource.Local();
            default -> new LabelSource.Unknown(kind);
        };
    }

    private static PortalShape shape(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new JsonParseException("missing 'shape'");
        }
        JsonObject shape = asObject(element, "'shape'");
        String kind = string(shape, TYPE);
        if (kind == null) {
            throw new JsonParseException("missing 'shape.type'");
        }
        return switch (kind) {
            case BOX -> new Box(point(shape, MIN), point(shape, MAX));
            case DISC ->
                new Disc(point(shape, CENTER), number(shape, RADIUS, null), point(shape, NORMAL));
            default -> throw new JsonParseException("unknown shape type '" + kind + "'");
        };
    }

    @Override
    public JsonElement serialize(Portal portal, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty(ID, portal.id());
        object.addProperty(TASK, portal.task());
        if (portal.permission() != null) {
            object.addProperty(PERMISSION, portal.permission());
        }
        object.add(SHAPE, shape(portal.shape()));
        if (portal.label() != null) {
            object.add(LABEL, label(portal.label()));
        }
        return object;
    }

    private static JsonObject label(PortalLabel label) {
        JsonObject object = new JsonObject();
        if (label.position() != null) {
            object.add(POSITION, point(label.position()));
        }
        object.addProperty(TEXT, label.text());
        if (label.offlineText() != null) {
            object.addProperty(OFFLINE_TEXT, label.offlineText());
        }
        if (label.source() != null) {
            object.add(SOURCE, source(label.source()));
        }
        object.addProperty(BILLBOARD, label.billboard().id());
        if (label.yaw() != 0f) {
            object.addProperty(YAW, label.yaw());
        }
        return object;
    }

    private static JsonObject source(LabelSource source) {
        JsonObject object = new JsonObject();
        switch (source) {
            case LabelSource.Task task -> named(object, TASK, task.name());
            case LabelSource.Group group -> named(object, GROUP, group.name());
            case LabelSource.Service service -> named(object, SERVICE, service.name());
            case LabelSource.Local _ -> object.addProperty(TYPE, LOCAL);
            case LabelSource.Unknown unknown -> object.addProperty(TYPE, unknown.type());
        }
        return object;
    }

    private static void named(JsonObject object, String type, String name) {
        object.addProperty(TYPE, type);
        if (name != null) {
            object.addProperty(NAME, name);
        }
    }

    private static JsonObject shape(PortalShape shape) {
        JsonObject object = new JsonObject();
        switch (shape) {
            case Box box -> {
                object.addProperty(TYPE, BOX);
                object.add(MIN, point(box.min()));
                object.add(MAX, point(box.max()));
            }
            case Disc disc -> {
                object.addProperty(TYPE, DISC);
                object.add(CENTER, point(disc.center()));
                object.addProperty(RADIUS, disc.radius());
                object.add(NORMAL, point(disc.normal()));
            }
        }
        return object;
    }

    private static JsonObject point(Vec vec) {
        JsonObject object = new JsonObject();
        object.addProperty(X, vec.x());
        object.addProperty(Y, vec.y());
        object.addProperty(Z, vec.z());
        return object;
    }

    private static Vec point(JsonObject parent, String name) {
        JsonObject point = asObject(parent.get(name), "'" + name + "'");
        return new Vec(number(point, X, name), number(point, Y, name), number(point, Z, name));
    }

    private static double number(JsonObject object, String name, String parent) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            throw new JsonParseException("missing '" + (parent == null ? name : parent + "." + name) + "'");
        }
        if (!value.isJsonPrimitive()) {
            throw new JsonParseException("'" + name + "' must be a number");
        }
        try {
            return value.getAsDouble();
        } catch (NumberFormatException exception) {
            throw new JsonParseException("'" + name + "' must be a number", exception);
        }
    }

    private static boolean isPresent(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && !value.isJsonNull();
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
