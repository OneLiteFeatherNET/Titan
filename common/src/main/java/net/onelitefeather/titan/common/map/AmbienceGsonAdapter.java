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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.ambience.AmbienceZone;
import net.onelitefeather.titan.core.portal.Box;

/**
 * Reads and writes the {@code ambience} entries of a {@code map.json}. Like the NPC adapter, a
 * missing or mistyped field stays empty for {@code AmbienceValidator} to report.
 */
final class AmbienceGsonAdapter implements JsonDeserializer<AmbienceZone>, JsonSerializer<AmbienceZone> {

    @Override
    public AmbienceZone deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = LenientJson.object(json);
        return new AmbienceZone(
                LenientJson.string(object, "id"), box(object == null ? null : object.get("area")), particles(object == null ? null : object.get("particles")), sounds(object == null ? null : object.get("sounds")), signs(object == null ? null : object.get("signs")), positions(object == null ? null : object.get("pages")), positions(object == null ? null : object.get("apparitions")), NpcGsonAdapter.skin(object == null ? null : object.get("apparitionSkin")));
    }

    @Override
    public JsonElement serialize(AmbienceZone zone, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("id", zone.id());
        if (zone.area() != null) {
            JsonObject area = new JsonObject();
            area.add("min", LenientJson.point(zone.area().min()));
            area.add("max", LenientJson.point(zone.area().max()));
            object.add("area", area);
        }
        if (zone.particles() != null) {
            JsonObject particles = new JsonObject();
            particles.addProperty("type", zone.particles().type());
            particles.addProperty("count", zone.particles().count());
            object.add("particles", particles);
        }
        if (zone.sounds() != null) {
            JsonObject sounds = new JsonObject();
            sounds.addProperty("key", zone.sounds().key());
            sounds.addProperty("minSeconds", zone.sounds().minSeconds());
            sounds.addProperty("maxSeconds", zone.sounds().maxSeconds());
            object.add("sounds", sounds);
        }
        if (!zone.signs().isEmpty()) {
            JsonArray signs = new JsonArray();
            zone.signs().forEach(sign -> signs.add(sign(sign)));
            object.add("signs", signs);
        }
        if (!zone.pages().isEmpty()) {
            object.add("pages", points(zone.pages()));
        }
        if (!zone.apparitions().isEmpty()) {
            object.add("apparitions", points(zone.apparitions()));
        }
        if (zone.apparitionSkin() != null) {
            object.add("apparitionSkin", NpcGsonAdapter.skin(zone.apparitionSkin()));
        }
        return object;
    }

    private static Box box(JsonElement element) {
        JsonObject object = LenientJson.object(element);
        Vec min = object == null ? null : LenientJson.vec(object.get("min"));
        Vec max = object == null ? null : LenientJson.vec(object.get("max"));
        return min == null || max == null ? null : new Box(min, max);
    }

    private static AmbienceZone.Particles particles(JsonElement element) {
        JsonObject object = LenientJson.object(element);
        return object == null ? null : new AmbienceZone.Particles(LenientJson.string(object, "type"), LenientJson.integer(object, "count"));
    }

    private static AmbienceZone.Sounds sounds(JsonElement element) {
        JsonObject object = LenientJson.object(element);
        return object == null ? null : new AmbienceZone.Sounds(LenientJson.string(object, "key"), LenientJson.decimal(object, "minSeconds"), LenientJson.decimal(object, "maxSeconds"));
    }

    private static List<AmbienceZone.Sign> signs(JsonElement element) {
        return LenientJson.elements(element).stream().map(AmbienceGsonAdapter::sign).toList();
    }

    private static AmbienceZone.Sign sign(JsonElement element) {
        JsonObject object = LenientJson.object(element);
        return new AmbienceZone.Sign(LenientJson.vec(positionOf(element)), LenientJson.string(object, "key"));
    }

    private static JsonObject sign(AmbienceZone.Sign sign) {
        JsonObject object = new JsonObject();
        object.add("position", LenientJson.point(sign.position()));
        object.addProperty("key", sign.key());
        return object;
    }

    /**
     * One position per entry, each in its own object with a {@code position}; a bad one is
     * {@code null}.
     */
    private static List<Vec> positions(JsonElement element) {
        return LenientJson.elements(element).stream().map(entry -> LenientJson.vec(positionOf(entry))).toList();
    }

    private static JsonElement positionOf(JsonElement entry) {
        JsonObject object = LenientJson.object(entry);
        return object == null ? null : object.get("position");
    }

    private static JsonArray points(List<Vec> points) {
        JsonArray array = new JsonArray();
        points.forEach(point -> {
            JsonObject entry = new JsonObject();
            entry.add("position", LenientJson.point(point));
            array.add(entry);
        });
        return array;
    }
}
