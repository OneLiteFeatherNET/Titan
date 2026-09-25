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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Migrates the lobby's original flat {@code app.json} format to the sectioned format {@link
 * AppJsonMigration} writes to {@code application.yaml}.
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
 * <li>{@code fireworkBoostSlot}, {@code updateRateAgones} and {@code elytraBoostMultiplier} are
 * dropped and logged - the ported {@code elytra} boost (see {@code design.md}, decision 7 of the
 * {@code lobby-feature-modules} change) has no multiplier to migrate it to. A migrated document
 * therefore gets no {@code elytra} section at all, and the module starts with its own compiled-in
 * defaults instead.</li>
 * </ul>
 */
final class LegacyConfigMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyConfigMigration.class);

    /**
     * Legacy top-level keys that identify a document as the old flat format. A document without
     * {@code configVersion} that has at least one of these keys is treated as version 1.
     */
    private static final Set<String> LEGACY_KEYS = Set.of(
            "tickleDuration", "sitOffset", "allowedSitBlocks", "simulationDistance", "fireworkBoostSlot", "elytraBoostMultiplier", "updateRateAgones", "minHeightBeforeTeleport", "maxHeightBeforeTeleport");

    /**
     * Legacy keys that are no longer read by any module and are dropped during migration.
     * {@code elytraBoostMultiplier} is dropped rather than migrated because the ported
     * {@code elytra} boost (see {@code design.md}, decision 7 of the {@code lobby-feature-modules}
     * change) has no multiplier - the impulse is Vanilla's own, applied client-side once a rocket
     * is attached to the player.
     */
    private static final List<String> DROPPED_KEYS = List.of("fireworkBoostSlot", "updateRateAgones", "elytraBoostMultiplier");

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
     * Maps a legacy flat document to the sectioned format, without logging the keys it drops -
     * that is the caller's business. {@link AppJsonMigration} uses this for the one-time
     * {@code app.json} &rarr; {@code application.yaml} switch.
     *
     * @param legacy   the parsed legacy document
     * @param fileName the name of the file being migrated, used only to complete a thrown {@link
     *                 ConfigException}
     * @return the migrated, sectioned document - {@code configVersion} was only ever a marker for
     *         {@link #isLegacy(JsonObject)} to read on the original {@code app.json}; it is a
     *         leftover of the old JSON format with no meaning in {@code application.yaml} and is
     *         never added to the result
     */
    static JsonObject toSectioned(JsonObject legacy, String fileName) {
        JsonObject migrated = new JsonObject();

        JsonObject spawn = new JsonObject();
        moveIfPresent(legacy, "simulationDistance", spawn, "simulationDistance");
        moveIfPresent(legacy, "minHeightBeforeTeleport", spawn, "minHeight");
        moveIfPresent(legacy, "maxHeightBeforeTeleport", spawn, "maxHeight");
        addIfNotEmpty(migrated, "spawn", spawn);

        JsonObject sit = new JsonObject();
        moveIfPresent(legacy, "sitOffset", sit, "offset");
        if (legacy.has("allowedSitBlocks")) {
            sit.add("allowedBlocks", migrateAllowedBlocks(legacy.getAsJsonArray("allowedSitBlocks"), fileName));
        }
        addIfNotEmpty(migrated, "sit", sit);

        JsonObject tickle = new JsonObject();
        moveIfPresent(legacy, "tickleDuration", tickle, "cooldownMillis");
        addIfNotEmpty(migrated, "tickle", tickle);

        return migrated;
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
     *
     * @param legacyBlocks the legacy array to convert
     * @param fileName     the name of the file being migrated, used only to complete a thrown
     *                     {@link ConfigException}
     * @throws ConfigException if a legacy {@code {"namespace":..,"value":..}} object is missing
     *                         its {@code value} key
     */
    private static JsonArray migrateAllowedBlocks(JsonArray legacyBlocks, String fileName) {
        JsonArray migrated = new JsonArray();
        for (JsonElement element : legacyBlocks) {
            if (element.isJsonObject()) {
                JsonObject block = element.getAsJsonObject();
                if (!block.has("value")) {
                    throw ConfigException.invalid("allowedBlocks", "a legacy allowedSitBlocks entry is missing 'value': " + block).withSection("sit").withFile(fileName);
                }
                String namespace = block.has("namespace") ? block.get("namespace").getAsString() : "minecraft";
                String value = block.get("value").getAsString();
                migrated.add(namespace + ":" + value);
            } else {
                migrated.add(element.getAsString());
            }
        }
        return migrated;
    }

    /**
     * Logs the legacy keys that {@code legacy} contains and {@link #toSectioned(JsonObject,
     * String)} drops instead of migrating. Package-private so {@link AppJsonMigration} can reuse
     * the exact same log line for the one-time {@code app.json} switch.
     *
     * @param file   the path the legacy document was read from, used only for the log message
     * @param legacy the parsed legacy document
     */
    static void logDroppedKeys(Path file, JsonObject legacy) {
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
