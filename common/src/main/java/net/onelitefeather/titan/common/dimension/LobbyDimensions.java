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
package net.onelitefeather.titan.common.dimension;

import net.kyori.adventure.key.Key;
import net.minestom.server.color.Color;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import net.minestom.server.world.clock.WorldClock;
import net.onelitefeather.titan.core.module.LobbyAtmosphere;

import java.util.Optional;

/**
 * Registers the dimension a lobby atmosphere needs. A dimension must be registered before a
 * player can join it, so the lobby registers it once at startup and creates its instance with it.
 */
public final class LobbyDimensions {

    private static final String NAMESPACE = "titan";

    // The slender fog: Granskoga's atmosphere, mixed 30 % from BRIGHT towards it, as Cygnus's
    // GameMapProvider does for the waiting lobby (lobbyAtmosphereShare). LobbyDimensionsTest derives them again.
    public static final Color FOG_COLOR = new Color(43, 129, 124);
    public static final Color SKY_LIGHT_COLOR = new Color(78, 165, 164);
    public static final Color SKY_COLOR = new Color(9, 27, 33);
    public static final Color AMBIENT_LIGHT_COLOR = new Color(5, 13, 13);
    public static final float SKY_LIGHT_FACTOR = 0.0362f;
    public static final float FOG_START_DISTANCE = 22.4f;
    public static final float FOG_END_DISTANCE = 189.4f;
    public static final float SKY_FOG_END_DISTANCE = 119.8f;
    // Cygnus fixes the sun and moon too; the fog's look depends on it.
    private static final float SUN_AND_MOON_ANGLE = 180f;

    private LobbyDimensions() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param registries the server's registries the dimension is registered in
     * @param atmosphere the atmosphere the lobby wears, if any
     * @return the key of the dimension to create the lobby instance with; the overworld without an
     *         atmosphere
     */
    public static RegistryKey<DimensionType> register(Registries registries, Optional<LobbyAtmosphere> atmosphere) {
        return atmosphere.map(value -> registerAtmosphere(registries, value)).orElse(DimensionType.OVERWORLD);
    }

    private static RegistryKey<DimensionType> registerAtmosphere(Registries registries, LobbyAtmosphere atmosphere) {
        return switch (atmosphere) {
            case SLENDER_FOG ->
                registries.dimensionType().register(Key.key(NAMESPACE, "lobby/" + atmosphere.key()), slenderFog(overworldClock(registries)));
        };
    }

    private static DimensionType slenderFog(RegistryKey<WorldClock> clock) {
        return DimensionType.builder().setAttribute(EnvironmentAttribute.FOG_START_DISTANCE, FOG_START_DISTANCE).setAttribute(EnvironmentAttribute.FOG_END_DISTANCE, FOG_END_DISTANCE).setAttribute(EnvironmentAttribute.SKY_FOG_END_DISTANCE, SKY_FOG_END_DISTANCE).setAttribute(EnvironmentAttribute.FOG_COLOR, FOG_COLOR).setAttribute(EnvironmentAttribute.SKY_COLOR, SKY_COLOR).setAttribute(EnvironmentAttribute.SKY_LIGHT_COLOR, SKY_LIGHT_COLOR).setAttribute(EnvironmentAttribute.SKY_LIGHT_FACTOR, SKY_LIGHT_FACTOR).setAttribute(EnvironmentAttribute.AMBIENT_LIGHT_COLOR, AMBIENT_LIGHT_COLOR).setAttribute(EnvironmentAttribute.SUN_ANGLE, SUN_AND_MOON_ANGLE).setAttribute(EnvironmentAttribute.MOON_ANGLE, SUN_AND_MOON_ANGLE)
                // The overworld's clock keeps the day cycle that daytime freezes and sets.
                .defaultClock(clock).build();
    }

    private static RegistryKey<WorldClock> overworldClock(Registries registries) {
        return registries.dimensionType().get(DimensionType.OVERWORLD).defaultClock();
    }
}
