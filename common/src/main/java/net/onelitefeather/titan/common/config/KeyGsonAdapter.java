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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.key.Key;

import java.io.IOException;

/**
 * Serializes a {@link Key} as a single JSON string, e.g. {@code "minecraft:spruce_stairs"}.
 * <p>
 * Aves' {@code net.theevilreaper.aves.file.gson.KeyGsonAdapter} represents a {@link Key} as an
 * object ({@code {"namespace":"minecraft","value":"spruce_stairs"}}), which is the format the
 * lobby's legacy {@code app.json} used. The sectioned config format defined for
 * {@code lobby-feature-modules} switched to the plain string form instead, so this store uses its
 * own adapter for it.
 */
final class KeyGsonAdapter extends TypeAdapter<Key> {

    @Override
    public void write(JsonWriter out, Key value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.asString());
    }

    @Override
    public Key read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return Key.key(in.nextString());
    }
}
