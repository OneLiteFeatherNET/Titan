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
package net.onelitefeather.titan.core.ambience;

import java.util.Set;

/**
 * The particles and sounds an ambience zone may use. The server ships no resource pack, so only
 * names the vanilla client already knows are accepted; a custom name would be silent or missing on
 * the client.
 */
public final class VanillaAmbience {

    /** Vanilla particle types, without the {@code minecraft:} namespace. */
    public static final Set<String> PARTICLES = Set.of("white_ash", "ash", "soul", "sculk_soul", "end_rod");

    /** Vanilla sound events, without the {@code minecraft:} namespace. */
    public static final Set<String> SOUNDS = Set.of("ambient.cave", "entity.enderman.stare", "ambient.soul_sand_valley.mood", "ambient.crimson_forest.mood");

    private static final String NAMESPACE = "minecraft:";

    private VanillaAmbience() {
    }

    /** Whether the name is a vanilla particle type, with or without {@code minecraft:}. */
    public static boolean isParticle(String name) {
        return PARTICLES.contains(bare(name));
    }

    /** Whether the name is a vanilla sound event, with or without {@code minecraft:}. */
    public static boolean isSound(String name) {
        return SOUNDS.contains(bare(name));
    }

    /** The name without the namespace; a name without one is returned as it is. */
    public static String bare(String name) {
        return name.startsWith(NAMESPACE) ? name.substring(NAMESPACE.length()) : name;
    }
}
