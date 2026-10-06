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
package net.onelitefeather.titan.feature.jumprun;

import io.avaje.config.Configuration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.key.InvalidKeyException;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.course.Palette;
import net.onelitefeather.titan.feature.jumprun.course.Palettes;
import net.onelitefeather.titan.feature.jumprun.course.Step;
import net.onelitefeather.titan.feature.jumprun.course.Surface;

/**
 * Parsing and validation of {@code jumprun.palettes.<shape>.<block>: <weight>} (weight 0 switches a
 * material off) and {@code
 * jumprun.<mode>.rerollTicks}. Every failure names the full key, so the operator finds the line to
 * fix.
 */
public final class JumprunSettings {

    static final String PALETTES_KEY = "jumprun.palettes";
    static final String RAINBOW_REROLL_TICKS_KEY = "jumprun.rainbow.rerollTicks";
    static final String ULTRA_REROLL_TICKS_KEY = "jumprun.ultra.rerollTicks";
    static final String HEAD_PROFILES_KEY = "jumprun.heads.profiles";

    private static final Pattern UUID_FORMAT = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** Tolerance for comparing collision heights, which are multiples of 1/16. */
    private static final double TOP_TOLERANCE = 1e-9;

    private JumprunSettings() {
    }

    public static String key(Surface surface) {
        return PALETTES_KEY + "." + surface.configKey();
    }

    public static String key(Surface surface, String block) {
        return key(surface) + "." + block;
    }

    /**
     * @throws IllegalArgumentException naming the first invalid key and the reason
     */
    public static Palettes palettes(Configuration config) {
        Map<Surface, Palette> byShape = new EnumMap<>(Surface.class);
        for (Surface surface : Surface.values()) {
            Configuration section = config.forPath(key(surface));
            Map<String, String> weights = new TreeMap<>();
            section.keys().forEach(block -> weights.put(block, section.get(block)));
            byShape.put(surface, palette(surface, weights));
        }
        return new Palettes(byShape);
    }

    /**
     * The player UUIDs of {@code jumprun.heads.profiles}, a comma separated list without
     * duplicates,
     * in the order of first appearance; empty when it is empty or missing.
     *
     * @throws IllegalArgumentException naming the key and the entry that is no UUID
     */
    static List<UUID> headProfiles(Configuration config) {
        Set<UUID> profiles = new LinkedHashSet<>();
        for (String entry : config.getOptional(HEAD_PROFILES_KEY).orElse("").split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!UUID_FORMAT.matcher(trimmed).matches()) {
                throw new IllegalArgumentException(HEAD_PROFILES_KEY + ": '" + trimmed + "' is not a UUID");
            }
            profiles.add(UUID.fromString(trimmed));
        }
        return List.copyOf(profiles);
    }

    /**
     * @throws IllegalArgumentException naming {@code key} when it is missing, not a whole number
     *                                  or not above 0
     */
    static int rerollTicks(Configuration config, String key) {
        String raw = config.getOptional(key).orElseThrow(() -> new IllegalArgumentException(key + ": missing, it needs a whole number above 0"));
        int ticks;
        try {
            ticks = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + ": must be a whole number, got '" + raw + "'");
        }
        if (ticks <= 0) {
            throw new IllegalArgumentException(key + ": must be greater than 0, got " + ticks);
        }
        return ticks;
    }

    /** Sorted by block name, so a seed draws the same material whatever order the config has. */
    static Palette palette(Surface surface, Map<String, String> weights) {
        if (weights.isEmpty()) {
            throw new IllegalArgumentException(key(surface) + " must not be empty - the shape could never be shown otherwise");
        }
        List<Palette.Weighted> entries = new ArrayList<>();
        // Every entry is validated, a switched-off one too, so a typo in its name still shows.
        new TreeMap<>(weights).forEach((name, weight) -> {
            Palette.Weighted entry = new Palette.Weighted(block(surface, name), weight(surface, name, weight));
            if (entry.weight() > 0) {
                entries.add(entry);
            }
        });
        if (entries.isEmpty()) {
            throw new IllegalArgumentException(key(surface) + " needs a material with a weight above 0 - the shape could never be shown otherwise");
        }
        return Palette.of(entries);
    }

    private static Block block(Surface surface, String name) {
        String key = key(surface, name);
        Block block;
        try {
            block = Block.fromKey("minecraft:" + name);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(key + ": invalid block name '" + name + "'");
        }
        if (block == null) {
            throw new IllegalArgumentException(key + ": unknown block '" + name + "'");
        }
        Block shaped = surface.shape(block);
        double top = shaped.collisionShape().relativeEnd().y();
        if (Math.abs(top - surface.top()) > TOP_TOLERANCE) {
            throw new IllegalArgumentException(key + ": collides up to " + top + " but the " + surface.configKey() + " shape needs " + surface.top());
        }
        if (surface.hasNarrowFootprint()) {
            requireFootprint(surface, key, shaped);
        }
        return shaped;
    }

    /** The landing check trusts the footprint of the shape, so a material must collide over it. */
    private static void requireFootprint(Surface surface, String key, Block shaped) {
        Point start = shaped.collisionShape().relativeStart();
        Point end = shaped.collisionShape().relativeEnd();
        Step wanted = surface.footprint();
        boolean matches = within(start.x(), wanted.minX()) && within(end.x(), wanted.maxX()) && within(start.z(), wanted.minZ()) && within(end.z(), wanted.maxZ());
        if (!matches) {
            throw new IllegalArgumentException(key + ": collides over x " + start.x() + ".." + end.x() + ", z " + start.z() + ".." + end.z() + " but the " + surface.configKey() + " shape needs x " + wanted.minX() + ".." + wanted.maxX() + ", z " + wanted.minZ() + ".." + wanted.maxZ());
        }
    }

    private static boolean within(double actual, double expected) {
        return Math.abs(actual - expected) <= TOP_TOLERANCE;
    }

    private static int weight(Surface surface, String name, String raw) {
        String key = key(surface, name);
        int weight;
        try {
            weight = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + ": weight must be a whole number, got '" + raw + "'");
        }
        if (weight < 0) {
            throw new IllegalArgumentException(key + ": weight must be 0 or greater, got " + weight);
        }
        return weight;
    }
}
