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
import java.lang.reflect.Type;
import net.onelitefeather.titan.core.npc.LobbyNpc;
import net.onelitefeather.titan.core.npc.NpcSkin;
import org.jetbrains.annotations.Nullable;

/**
 * Reads and writes the {@code npcs} entries of a {@code map.json}. Missing or mistyped fields stay
 * {@code null} for {@code NpcValidator} to report; only the label is read strictly, as for portals.
 */
final class NpcGsonAdapter implements JsonDeserializer<LobbyNpc>, JsonSerializer<LobbyNpc> {

    @Override
    public LobbyNpc deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = LenientJson.object(json);
        return new LobbyNpc(
                LenientJson.string(object, "id"), LenientJson.string(object, "task"), LenientJson.pos(object == null ? null : object.get("position")), skin(object == null ? null : object.get("skin")), PortalGsonAdapter.label(object == null ? null : object.get("label")), LenientJson.bool(object, "lookAtPlayers"), LenientJson.string(object, "permission"));
    }

    @Override
    public JsonElement serialize(LobbyNpc npc, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("id", npc.id());
        object.addProperty("task", npc.task());
        if (npc.position() != null) {
            JsonObject position = LenientJson.point(npc.position());
            position.addProperty("yaw", npc.position().yaw());
            position.addProperty("pitch", npc.position().pitch());
            object.add("position", position);
        }
        if (npc.skin() != null) {
            object.add("skin", skin(npc.skin()));
        }
        if (npc.label() != null) {
            object.add("label", PortalGsonAdapter.label(npc.label()));
        }
        if (npc.lookAtPlayers()) {
            object.addProperty("lookAtPlayers", true);
        }
        if (npc.permission() != null) {
            object.addProperty("permission", npc.permission());
        }
        return object;
    }

    /** The {@code skin} object: {@code value} and {@code signature}; {@code null} when absent. */
    static @Nullable NpcSkin skin(@Nullable JsonElement element) {
        JsonObject object = LenientJson.object(element);
        if (object == null) {
            return null;
        }
        return new NpcSkin(LenientJson.string(object, "value"), LenientJson.string(object, "signature"));
    }

    static JsonObject skin(NpcSkin skin) {
        JsonObject object = new JsonObject();
        object.addProperty("value", skin.value());
        if (skin.signature() != null) {
            object.addProperty("signature", skin.signature());
        }
        return object;
    }
}
