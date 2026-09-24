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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Migrates the lobby's original flat {@code app.json} format to the sectioned format used by
 * {@link ConfigStore}.
 * <p>
 * The mapping is fixed (see {@code design.md}, decision 5 of the {@code lobby-feature-modules}
 * change):
 * <ul>
 * <li>{@code tickleDuration} &rarr; {@code tickle.cooldownMillis}</li>
 * <li>{@code sitOffset} &rarr; {@code sit.offset}</li>
 * <li>{@code allowedSitBlocks} &rarr; {@code sit.allowedBlocks} (accepts both the legacy
 * {@code {"namespace":..,"value":..}} objects and plain strings, always writes strings)</li>
 * <li>{@code simulationDistance} &rarr; {@code spawn.simulationDistance}</li>
 * <li>{@code minHeightBeforeTeleport} &rarr; {@code spawn.minHeight}</li>
 * <li>{@code maxHeightBeforeTeleport} &rarr; {@code spawn.maxHeight}</li>
 * <li>{@code elytraBoostMultiplier} &rarr; {@code elytra.boostMultiplier}</li>
 * <li>{@code fireworkBoostSlot} and {@code updateRateAgones} are dropped and logged</li>
 * </ul>
 * Before the migrated document is written, the original file is copied next to itself as
 * {@code <file>.v1.bak}.
 */
final class LegacyConfigMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyConfigMigration.class);

    static final int TARGET_CONFIG_VERSION = 2;

    /**
     * Legacy top-level keys that identify a document as the old flat format. A document without
     * {@code configVersion} that has at least one of these keys is treated as version 1.
     */
    private static final Set<String> LEGACY_KEYS = Set.of(
            "tickleDuration", "sitOffset", "allowedSitBlocks", "simulationDistance", "fireworkBoostSlot", "elytraBoostMultiplier", "updateRateAgones", "minHeightBeforeTeleport", "maxHeightBeforeTeleport");

    /**
     * Legacy keys that are no longer read by any module and are dropped during migration.
     */
    private static final List<String> DROPPED_KEYS = List.of("fireworkBoostSlot", "updateRateAgones");

    private LegacyConfigMigration() {
    }

    /**
     * Determines whether a parsed document is the legacy flat format: no {@code configVersion}
     * field and at least one recognized legacy key.
     */
    static boolean isLegacy(JsonObject document) {
        if (document.has("configVersion")) {
            return false;
        }
        for (String key : LEGACY_KEYS) {
            if (document.has(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Migrates a legacy flat document read from {@code file} into the sectioned format.
     * Backs up the original file to {@code <file>.v1.bak} before returning, and logs the
     * legacy keys that were dropped instead of migrated.
     *
     * @param file   the path the legacy document was read from, used only for the backup copy
     *               and log message
     * @param legacy the parsed legacy document
     * @return the migrated, sectioned document, including {@code configVersion}
     */
    static JsonObject migrate(Path file, JsonObject legacy) {
        backUp(file);

        JsonObject migrated = new JsonObject();
        migrated.addProperty("configVersion", TARGET_CONFIG_VERSION);

        JsonObject spawn = new JsonObject();
        moveIfPresent(legacy, "simulationDistance", spawn, "simulationDistance");
        moveIfPresent(legacy, "minHeightBeforeTeleport", spawn, "minHeight");
        moveIfPresent(legacy, "maxHeightBeforeTeleport", spawn, "maxHeight");
        addIfNotEmpty(migrated, "spawn", spawn);

        JsonObject sit = new JsonObject();
        moveIfPresent(legacy, "sitOffset", sit, "offset");
        if (legacy.has("allowedSitBlocks")) {
            sit.add("allowedBlocks", migrateAllowedBlocks(legacy.getAsJsonArray("allowedSitBlocks")));
        }
        addIfNotEmpty(migrated, "sit", sit);

        JsonObject tickle = new JsonObject();
        moveIfPresent(legacy, "tickleDuration", tickle, "cooldownMillis");
        addIfNotEmpty(migrated, "tickle", tickle);

        JsonObject elytra = new JsonObject();
        moveIfPresent(legacy, "elytraBoostMultiplier", elytra, "boostMultiplier");
        addIfNotEmpty(migrated, "elytra", elytra);

        logDroppedKeys(file, legacy);

        return migrated;
    }

    private static void backUp(Path file) {
        Path backup = file.resolveSibling(file.getFileName().toString() + ".v1.bak");
        try {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to back up legacy config " + file + " to " + backup, e);
        }
    }

    private static void moveIfPresent(JsonObject source, String sourceKey, JsonObject target, String targetKey) {
        if (source.has(sourceKey)) {
            target.add(targetKey, source.get(sourceKey));
        }
    }

    private static void addIfNotEmpty(JsonObject target, String key, JsonObject section) {
        if (!section.entrySet().isEmpty()) {
            target.add(key, section);
        }
    }

    /**
     * Converts the legacy {@code allowedSitBlocks} array to the new string form. Each element may
     * already be a plain string (new form, tolerated on repeated migration attempts) or a legacy
     * {@code {"namespace":..,"value":..}} object.
     */
    private static JsonArray migrateAllowedBlocks(JsonArray legacyBlocks) {
        JsonArray migrated = new JsonArray();
        for (JsonElement element : legacyBlocks) {
            if (element.isJsonObject()) {
                JsonObject block = element.getAsJsonObject();
                String namespace = block.has("namespace") ? block.get("namespace").getAsString() : "minecraft";
                String value = block.get("value").getAsString();
                migrated.add(namespace + ":" + value);
            } else {
                migrated.add(element.getAsString());
            }
        }
        return migrated;
    }

    private static void logDroppedKeys(Path file, JsonObject legacy) {
        List<String> dropped = new ArrayList<>();
        for (String key : DROPPED_KEYS) {
            if (legacy.has(key)) {
                dropped.add(key);
            }
        }
        if (!dropped.isEmpty()) {
            LOGGER.warn("Dropped unused legacy config keys while migrating {}: {}", file.getFileName(), String.join(", ", dropped));
        }
    }
}
