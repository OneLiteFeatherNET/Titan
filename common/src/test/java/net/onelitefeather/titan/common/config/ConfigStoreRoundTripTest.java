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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the {@code lobby-module-config} spec scenarios "round trip without change" and "changes
 * from the setup server don't lose values": {@link ConfigStore#save()} must not disturb sections
 * or fields it was not asked to change.
 */
class ConfigStoreRoundTripTest {

    @Test
    @DisplayName("Loading and saving without changes leaves the file content-equal")
    void loadAndSaveWithoutChangesIsContentEqual(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");

        ConfigStore first = ConfigStore.open(file);
        first.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        first.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);
        first.flush();
        String firstContent = Files.readString(file);

        // A fresh store loading the very file we just wrote, requesting the same sections with
        // the same defaults: the file already has every value, so nothing should change.
        ConfigStore second = ConfigStore.open(file);
        second.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        second.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS);
        second.save();
        String secondContent = Files.readString(file);

        assertEquals(firstContent, secondContent, "an unmodified round trip must be content-equal");
    }

    @Test
    @DisplayName("set() on one field keeps every other section and field content-equal")
    void setOnOneFieldKeepsRestContentEqual(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");

        ConfigStore setup = ConfigStore.open(file);
        setup.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        SpawnTestConfig customSpawn = new SpawnTestConfig(-32, 400, 4);
        setup.setSection("spawn", customSpawn);
        setup.flush();

        JsonObject before = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

        ConfigStore store = ConfigStore.open(file);
        store.set("spawn", "minHeight", new JsonPrimitive(-100));
        store.save();

        JsonObject after = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

        // The other section is completely untouched.
        assertEquals(before.get("tickle"), after.get("tickle"), "other sections must stay unchanged");
        // Within the changed section, only the touched field changed.
        assertEquals(-100, after.getAsJsonObject("spawn").get("minHeight").getAsInt());
        assertEquals(before.getAsJsonObject("spawn").get("maxHeight"), after.getAsJsonObject("spawn").get("maxHeight"));
        assertEquals(before.getAsJsonObject("spawn").get("simulationDistance"), after.getAsJsonObject("spawn").get("simulationDistance"));
    }
}
