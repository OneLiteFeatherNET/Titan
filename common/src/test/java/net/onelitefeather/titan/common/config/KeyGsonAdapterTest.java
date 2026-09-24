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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link KeyGsonAdapter}: a {@link Key} must round-trip as the plain string form (e.g.
 * {@code "minecraft:spruce_stairs"}), not Aves' {@code {"namespace":..,"value":..}} object, and an
 * invalid key string must fail with adventure's own, descriptive {@link InvalidKeyException}
 * rather than something opaque.
 */
class KeyGsonAdapterTest {

    private final KeyGsonAdapter adapter = new KeyGsonAdapter();

    @Test
    @DisplayName("A key round-trips through the plain string form")
    void roundTripsAsPlainString() {
        Key key = Key.key("minecraft:spruce_stairs");

        JsonElement written = adapter.toJsonTree(key);

        assertEquals(new JsonPrimitive("minecraft:spruce_stairs"), written, "the key must be written as a plain JSON string");
        assertEquals(key, adapter.fromJsonTree(written), "reading the written value back must reproduce the same key");
    }

    @Test
    @DisplayName("null round-trips through JSON null")
    void roundTripsNull() {
        JsonElement written = adapter.toJsonTree(null);

        assertEquals(JsonNull.INSTANCE, written);
        assertNull(adapter.fromJsonTree(written));
    }

    @Test
    @DisplayName("An invalid key string fails with adventure's own, clear error")
    void invalidKeyStringFailsClearly() {
        JsonElement invalid = new JsonPrimitive("Not A Valid Key!!");

        InvalidKeyException exception = assertThrows(InvalidKeyException.class, () -> adapter.fromJsonTree(invalid));

        assertEquals("Not A Valid Key!!", exception.keyValue());
    }
}
