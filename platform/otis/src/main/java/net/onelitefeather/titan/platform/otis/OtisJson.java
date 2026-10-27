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
import net.onelitefeather.titan.core.settings.SettingValue;

/**
 * The two JSON shapes the adapter needs from Otis: the list of stored settings it reads, and the
 * value body it writes. Fields the adapter does not use are ignored. A boolean becomes a flag and a
 * whole number a level; text and fractions are dropped, as is any entry under an invalid key.
 */
final class OtisJson {

    private OtisJson() {
    }

    /** @param body a JSON array of {@code {key, value, version, updatedAt}} entries */
    static Map<SettingKey, SettingValue> parseSettings(String body) {
        JsonArray entries = JsonParser.parseString(body).getAsJsonArray();
        Map<SettingKey, SettingValue> values = new HashMap<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            Optional<SettingKey> key = keyOf(entry);
            Optional<SettingValue> value = valueOf(entry.get("value"));
            if (key.isPresent() && value.isPresent()) {
                values.put(key.get(), value.get());
            }
        }
        return Map.copyOf(values);
    }

    /**
     * @return the body that sets a value: {@code {"value": true}} for a flag, {@code {"value": 75}}
     *         for a level
     */
    static String valueBody(SettingValue value) {
        JsonObject body = new JsonObject();
        switch (value) {
            case SettingValue.Flag flag -> body.addProperty("value", flag.enabled());
            case SettingValue.Level level -> body.addProperty("value", level.value());
        }
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

    private static Optional<SettingValue> valueOf(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) {
            return Optional.empty();
        }
        if (value.getAsJsonPrimitive().isBoolean()) {
            return Optional.of(new SettingValue.Flag(value.getAsBoolean()));
        }
        if (value.getAsJsonPrimitive().isNumber()) {
            return wholeNumber(value.getAsString());
        }
        return Optional.empty();
    }

    // Only a whole number is a level: "75" yes, "75.0" and "1e2" no, and a number outside int is no level.
    private static Optional<SettingValue> wholeNumber(String literal) {
        if (!literal.matches("-?\\d{1,10}")) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SettingValue.Level(Integer.parseInt(literal)));
        } catch (NumberFormatException outOfRange) {
            return Optional.empty();
        }
    }
}
