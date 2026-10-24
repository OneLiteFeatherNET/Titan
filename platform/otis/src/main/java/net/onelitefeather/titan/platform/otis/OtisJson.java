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
package net.onelitefeather.titan.platform.otis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.onelitefeather.titan.core.settings.SettingKey;

/**
 * The two JSON shapes the adapter needs from Otis: the list of stored settings it reads, and the
 * value body it writes. Fields the adapter does not use are ignored, and so are entries that are
 * not a boolean under a valid key.
 */
final class OtisJson {

    private OtisJson() {
    }

    /** @param body a JSON array of {@code {key, value, version, updatedAt}} entries */
    static Map<SettingKey, Boolean> parseSettings(String body) {
        JsonArray entries = JsonParser.parseString(body).getAsJsonArray();
        Map<SettingKey, Boolean> values = new HashMap<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            Optional<SettingKey> key = keyOf(entry);
            Optional<Boolean> value = booleanOf(entry.get("value"));
            if (key.isPresent() && value.isPresent()) {
                values.put(key.get(), value.get());
            }
        }
        return Map.copyOf(values);
    }

    /** @return the body that sets a value: {@code {"value": true}} */
    static String valueBody(boolean enabled) {
        JsonObject body = new JsonObject();
        body.addProperty("value", enabled);
        return body.toString();
    }

    private static Optional<SettingKey> keyOf(JsonObject entry) {
        JsonElement key = entry.get("key");
        if (key == null || !key.isJsonPrimitive() || !key.getAsJsonPrimitive().isString()) {
            return Optional.empty();
        }
        try {
            return Optional.of(SettingKey.parse(key.getAsString()));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    private static Optional<Boolean> booleanOf(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            return Optional.empty();
        }
        return Optional.of(value.getAsBoolean());
    }
}
