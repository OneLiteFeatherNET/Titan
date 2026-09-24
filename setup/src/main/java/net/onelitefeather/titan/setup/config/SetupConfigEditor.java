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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.common.config.ConfigException;
import net.onelitefeather.titan.common.config.ConfigStore;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Edits the {@code app.json} sections the setup server's {@code /setup app} command exposes,
 * one field at a time, through a {@link ConfigStore}.
 * <p>
 * Every mutating method here changes exactly one field of one section and immediately persists
 * it with {@link ConfigStore#set(String, String, JsonElement)} followed by {@link
 * ConfigStore#save()}. Because {@code set} only ever replaces the single field it is given, every
 * other section - and every other field of the same section, e.g. {@code spawn.minHeight} and
 * {@code spawn.maxHeight} when only {@code spawn.simulationDistance} changes - is written back
 * unchanged. This is what fixes the old copy-builder bug, where rebuilding the whole config from
 * a stale in-memory copy silently reset the height bounds.
 * <p>
 * This class has no dependency on a running Minestom server and is deliberately kept free of
 * command-parsing concerns, so it can be unit tested directly; {@code AppCommand} only parses
 * arguments and calls it.
 */
public final class SetupConfigEditor {

    private static final String SPAWN_SECTION = "spawn";
    private static final String SIT_SECTION = "sit";
    private static final String TICKLE_SECTION = "tickle";
    private static final String ELYTRA_SECTION = "elytra";

    private final ConfigStore store;

    public SetupConfigEditor(ConfigStore store) {
        this.store = store;
    }

    /**
     * Reads the current {@code spawn} section, falling back to {@link SpawnSectionConfig#DEFAULTS}
     * for any field missing from the file.
     */
    public SpawnSectionConfig spawn() {
        return store.section(SPAWN_SECTION, SpawnSectionConfig.class, SpawnSectionConfig.DEFAULTS);
    }

    /**
     * Reads the current {@code sit} section, falling back to {@link SitSectionConfig#DEFAULTS} for
     * any field missing from the file.
     */
    public SitSectionConfig sit() {
        return store.section(SIT_SECTION, SitSectionConfig.class, SitSectionConfig.DEFAULTS);
    }

    /**
     * Reads the current {@code tickle} section, falling back to {@link
     * TickleSectionConfig#DEFAULTS} for any field missing from the file.
     */
    public TickleSectionConfig tickle() {
        return store.section(TICKLE_SECTION, TickleSectionConfig.class, TickleSectionConfig.DEFAULTS);
    }

    /**
     * Reads the current {@code elytra} section, falling back to {@link
     * ElytraSectionConfig#DEFAULTS} for any field missing from the file.
     */
    public ElytraSectionConfig elytra() {
        return store.section(ELYTRA_SECTION, ElytraSectionConfig.class, ElytraSectionConfig.DEFAULTS);
    }

    /**
     * Sets {@code sit.offset}, leaving {@code sit.allowedBlocks} and every other section (in
     * particular {@code spawn.minHeight} / {@code spawn.maxHeight}) untouched.
     */
    public void setSitOffset(Vec offset) {
        store.set(SIT_SECTION, "offset", vecToJson(offset));
        store.save();
    }

    /**
     * Adds {@code block} to {@code sit.allowedBlocks} if it is not already present. Adding a
     * block that is already allowed is a no-op: the set of allowed blocks never contains
     * duplicates.
     */
    public void addAllowedSitBlock(Key block) {
        Set<Key> blocks = new LinkedHashSet<>(sit().allowedBlocks());
        blocks.add(block);
        store.set(SIT_SECTION, "allowedBlocks", keysToJson(blocks));
        store.save();
    }

    /**
     * Removes {@code block} from {@code sit.allowedBlocks}. Removing a block that is not present
     * is a no-op.
     */
    public void removeAllowedSitBlock(Key block) {
        Set<Key> blocks = new LinkedHashSet<>(sit().allowedBlocks());
        blocks.remove(block);
        store.set(SIT_SECTION, "allowedBlocks", keysToJson(blocks));
        store.save();
    }

    /**
     * Sets {@code spawn.simulationDistance}, leaving {@code spawn.minHeight} and {@code
     * spawn.maxHeight} untouched.
     */
    public void setSimulationDistance(int simulationDistance) {
        store.set(SPAWN_SECTION, "simulationDistance", new JsonPrimitive(simulationDistance));
        store.save();
    }

    /**
     * Sets {@code tickle.cooldownMillis}.
     */
    public void setTickleCooldownMillis(long cooldownMillis) {
        store.set(TICKLE_SECTION, "cooldownMillis", new JsonPrimitive(cooldownMillis));
        store.save();
    }

    /**
     * Sets {@code elytra.burnDurationTicks}, leaving {@code elytra.cooldownTicks} untouched.
     *
     * @throws ConfigException if the resulting pair - this new {@code burnDurationTicks} together
     *                         with the current {@code cooldownTicks} - is one the lobby's own
     *                         {@code ElytraConfig} would refuse to load; nothing is written to
     *                         {@code app.json} in that case
     */
    public void setElytraBurnDurationTicks(int burnDurationTicks) {
        ElytraSectionConfig resultingPair = validatedElytra(burnDurationTicks, elytra().cooldownTicks());
        store.set(ELYTRA_SECTION, "burnDurationTicks", new JsonPrimitive(resultingPair.burnDurationTicks()));
        store.save();
    }

    /**
     * Sets {@code elytra.cooldownTicks}, leaving {@code elytra.burnDurationTicks} untouched.
     *
     * @throws ConfigException if the resulting pair - the current {@code burnDurationTicks}
     *                         together with this new {@code cooldownTicks} - is one the lobby's
     *                         own {@code ElytraConfig} would refuse to load; nothing is written
     *                         to {@code app.json} in that case
     */
    public void setElytraCooldownTicks(int cooldownTicks) {
        ElytraSectionConfig resultingPair = validatedElytra(elytra().burnDurationTicks(), cooldownTicks);
        store.set(ELYTRA_SECTION, "cooldownTicks", new JsonPrimitive(resultingPair.cooldownTicks()));
        store.save();
    }

    /**
     * Validates {@code burnDurationTicks}/{@code cooldownTicks} against the same rule the lobby's
     * own {@code net.onelitefeather.titan.app.feature.elytra.ElytraConfig} enforces, by
     * constructing an {@link ElytraSectionConfig} - whose compact constructor carries that same
     * rule, mirrored because the setup server does not depend on {@code app} -
     * <strong>before</strong>
     * either {@link #setElytraBurnDurationTicks} or {@link #setElytraCooldownTicks} writes
     * anything, so a pair the lobby would refuse to load is rejected here instead of committed to
     * {@code app.json}.
     *
     * @throws ConfigException naming the offending field and the reason, unchanged from {@link
     *                         ElytraSectionConfig}'s own compact constructor
     */
    private static ElytraSectionConfig validatedElytra(int burnDurationTicks, int cooldownTicks) {
        return new ElytraSectionConfig(burnDurationTicks, cooldownTicks);
    }

    /**
     * Builds the {@code {"x":..,"y":..,"z":..}} shape {@link ConfigStore}'s {@code Vec} adapter
     * expects, matching {@code net.theevilreaper.aves.file.gson.PositionGsonAdapter}.
     */
    private static JsonElement vecToJson(Vec vec) {
        JsonObject json = new JsonObject();
        json.addProperty("x", vec.x());
        json.addProperty("y", vec.y());
        json.addProperty("z", vec.z());
        return json;
    }

    /**
     * Builds the plain-string array shape {@link ConfigStore}'s {@code Key} adapter expects, e.g.
     * {@code ["minecraft:spruce_stairs"]}.
     */
    private static JsonElement keysToJson(Iterable<Key> keys) {
        JsonArray array = new JsonArray();
        for (Key key : keys) {
            array.add(key.asString());
        }
        return array;
    }
}
