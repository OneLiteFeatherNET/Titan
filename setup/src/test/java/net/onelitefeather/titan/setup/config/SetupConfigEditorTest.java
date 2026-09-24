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
package net.onelitefeather.titan.setup.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.common.config.ConfigStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code lobby-module-config} spec scenario "Changes from the setup server don't lose
 * values" for the setup server's field-editing logic, plus the field-specific edits {@code
 * AppCommand} delegates to {@link SetupConfigEditor}.
 */
class SetupConfigEditorTest {

    @Test
    @DisplayName("Changing the sit offset keeps non-default spawn heights and other sections unchanged")
    void changingSitOffsetKeepsSpawnHeightsAndOtherSectionsUnchanged(@TempDir Path tempDir) {
        Path file = tempDir.resolve("app.json");

        // Seed the file with non-default spawn heights and elytra tuning, the way the lobby
        // itself would have written them.
        ConfigStore seed = ConfigStore.open(file);
        seed.setSection("spawn", new SpawnSectionConfig(-32, 400, 4));
        seed.setSection("elytra", new ElytraSectionConfig(20, 50));
        seed.flush();

        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));
        editor.setSitOffset(new Vec(1.0, 2.0, 3.0));

        SetupConfigEditor verify = new SetupConfigEditor(ConfigStore.open(file));
        assertEquals(new Vec(1.0, 2.0, 3.0), verify.sit().offset(), "the new offset must be stored");
        assertEquals(-32, verify.spawn().minHeight(), "the old copy-builder bug reset this to the default");
        assertEquals(400, verify.spawn().maxHeight(), "the old copy-builder bug reset this to the default");
        assertEquals(4, verify.spawn().simulationDistance());
        assertEquals(20, verify.elytra().burnDurationTicks(), "an unrelated section must stay untouched");
        assertEquals(50, verify.elytra().cooldownTicks(), "an unrelated section must stay untouched");
    }

    @Test
    @DisplayName("Changing the simulation distance keeps non-default spawn heights unchanged")
    void changingSimulationDistanceKeepsSpawnHeightsUnchanged(@TempDir Path tempDir) {
        Path file = tempDir.resolve("app.json");

        ConfigStore seed = ConfigStore.open(file);
        seed.setSection("spawn", new SpawnSectionConfig(-32, 400, 4));
        seed.flush();

        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));
        editor.setSimulationDistance(8);

        SetupConfigEditor verify = new SetupConfigEditor(ConfigStore.open(file));
        assertEquals(8, verify.spawn().simulationDistance());
        assertEquals(-32, verify.spawn().minHeight());
        assertEquals(400, verify.spawn().maxHeight());
    }

    @Test
    @DisplayName("Adding an allowed sit block twice does not duplicate it")
    void addingAllowedSitBlockTwiceDoesNotDuplicate(@TempDir Path tempDir) {
        Path file = tempDir.resolve("app.json");
        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));
        Key oakStairs = Key.key("minecraft:oak_stairs");

        editor.addAllowedSitBlock(oakStairs);
        editor.addAllowedSitBlock(oakStairs);

        List<Key> allowedBlocks = editor.sit().allowedBlocks();
        assertEquals(1, allowedBlocks.stream().filter(oakStairs::equals).count());
        assertTrue(allowedBlocks.contains(Key.key("minecraft:spruce_stairs")), "the pre-existing default block must survive the add");
    }

    @Test
    @DisplayName("Removing an allowed sit block removes it and only it")
    void removingAllowedSitBlockRemovesOnlyThatBlock(@TempDir Path tempDir) {
        Path file = tempDir.resolve("app.json");
        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));
        Key oakStairs = Key.key("minecraft:oak_stairs");
        editor.addAllowedSitBlock(oakStairs);

        editor.removeAllowedSitBlock(oakStairs);

        List<Key> allowedBlocks = editor.sit().allowedBlocks();
        assertFalse(allowedBlocks.contains(oakStairs));
        assertTrue(allowedBlocks.contains(Key.key("minecraft:spruce_stairs")));
    }

    @Test
    @DisplayName("Removing an allowed sit block that is not present is a no-op")
    void removingAbsentAllowedSitBlockIsNoop(@TempDir Path tempDir) {
        Path file = tempDir.resolve("app.json");
        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));

        editor.removeAllowedSitBlock(Key.key("minecraft:oak_stairs"));

        assertEquals(List.of(Key.key("minecraft:spruce_stairs")), editor.sit().allowedBlocks());
    }

    @Test
    @DisplayName("The tickle duration is written to tickle.cooldownMillis")
    void tickleDurationIsWrittenToCooldownMillis(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));

        editor.setTickleCooldownMillis(9000L);

        JsonObject document = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(9000L, document.getAsJsonObject("tickle").get("cooldownMillis").getAsLong());
        assertEquals(9000L, editor.tickle().cooldownMillis());
    }

    @Test
    @DisplayName("The elytra burn duration is written to elytra.burnDurationTicks, leaving cooldownTicks untouched")
    void elytraBurnDurationTicksIsWrittenToBurnDurationTicks(@TempDir Path tempDir) {
        Path file = tempDir.resolve("app.json");
        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));

        editor.setElytraBurnDurationTicks(20);

        assertEquals(20, editor.elytra().burnDurationTicks());
        assertEquals(ElytraSectionConfig.DEFAULTS.cooldownTicks(), editor.elytra().cooldownTicks());
    }

    @Test
    @DisplayName("The elytra cooldown is written to elytra.cooldownTicks, leaving burnDurationTicks untouched")
    void elytraCooldownTicksIsWrittenToCooldownTicks(@TempDir Path tempDir) {
        Path file = tempDir.resolve("app.json");
        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));

        editor.setElytraCooldownTicks(50);

        assertEquals(50, editor.elytra().cooldownTicks());
        assertEquals(ElytraSectionConfig.DEFAULTS.burnDurationTicks(), editor.elytra().burnDurationTicks());
    }

    @Test
    @DisplayName("A legacy flat app.json is migrated when the setup server opens it")
    void legacyFlatAppJsonIsMigratedWhenOpened(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "tickleDuration": 5000,
                  "sitOffset": {"x": 0.1, "y": 0.2, "z": 0.3},
                  "allowedSitBlocks": ["minecraft:oak_stairs"],
                  "simulationDistance": 6,
                  "minHeightBeforeTeleport": -50,
                  "maxHeightBeforeTeleport": 200,
                  "elytraBoostMultiplier": 12.5,
                  "fireworkBoostSlot": 45
                }
                """);

        SetupConfigEditor editor = new SetupConfigEditor(ConfigStore.open(file));

        assertEquals(5000L, editor.tickle().cooldownMillis());
        assertEquals(new Vec(0.1, 0.2, 0.3), editor.sit().offset());
        assertEquals(List.of(Key.key("minecraft:oak_stairs")), editor.sit().allowedBlocks());
        assertEquals(6, editor.spawn().simulationDistance());
        assertEquals(-50, editor.spawn().minHeight());
        assertEquals(200, editor.spawn().maxHeight());
        // elytraBoostMultiplier has no equivalent in the ported Voyager boost; it is dropped, not
        // migrated, so the section falls back to its defaults.
        assertEquals(ElytraSectionConfig.DEFAULTS.burnDurationTicks(), editor.elytra().burnDurationTicks());
        assertEquals(ElytraSectionConfig.DEFAULTS.cooldownTicks(), editor.elytra().cooldownTicks());
    }
}
