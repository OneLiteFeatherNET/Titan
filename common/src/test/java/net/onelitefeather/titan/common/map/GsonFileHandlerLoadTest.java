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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import net.theevilreaper.aves.file.GsonFileHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the Aves behaviour the portal loading relies on: a parse error of a type adapter must
 * surface from {@code load}, not turn into an empty result that would silently start a lobby
 * without spawn and portals (design D6).
 */
class GsonFileHandlerLoadTest {

    @TempDir
    Path directory;

    @DisplayName("GsonFileHandler.load lets a JsonParseException of an adapter escape instead of returning empty")
    @Test
    void loadPropagatesAdapterParseErrors() throws IOException {
        JsonParseException failure = new JsonParseException("broken adapter input");
        Gson gson = new GsonBuilder().registerTypeAdapter(Marker.class, (JsonDeserializer<Marker>) (json, type, context) -> {
            throw failure;
        }).create();
        Path file = Files.writeString(this.directory.resolve("map.json"), "{\"marker\":{}}");

        JsonParseException thrown = assertThrows(JsonParseException.class, () -> new GsonFileHandler(gson).load(file, Holder.class), "a parse error must not be swallowed into Optional.empty()");

        assertSame(failure, thrown, "the adapter's own exception must reach the caller");
    }

    private static final class Marker {
    }

    private static final class Holder {
        private Marker marker;
    }
}
