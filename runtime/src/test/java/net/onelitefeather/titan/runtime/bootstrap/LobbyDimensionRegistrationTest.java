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
package net.onelitefeather.titan.runtime.bootstrap;

import java.util.Optional;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.dimension.LobbyDimensions;
import net.onelitefeather.titan.core.module.LobbyAtmosphere;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class LobbyDimensionRegistrationTest {

    private static Object argumentOf(DimensionType dimension, EnvironmentAttribute<?> attribute) {
        return dimension.attributes().entries().get(attribute).argument();
    }

    @DisplayName("A season with the slender fog gives the lobby the fog dimension")
    @Test
    void fogSeasonUsesTheFogDimension(Env env) {
        InstanceContainer lobby = PlatformBeans.createLobbyInstance(env.process(), env.process().instance(), Optional.of(LobbyAtmosphere.SLENDER_FOG));

        Assertions.assertNotEquals(DimensionType.OVERWORLD, lobby.getDimensionType(), "the lobby must leave the overworld for the fog dimension");
        DimensionType dimension = lobby.getCachedDimensionType();
        Assertions.assertEquals(LobbyDimensions.FOG_END_DISTANCE, argumentOf(dimension, EnvironmentAttribute.FOG_END_DISTANCE), "the registered dimension must carry the fog end distance");
    }

    @DisplayName("The fog dimension keeps the overworld day clock")
    @Test
    void fogDimensionKeepsTheOverworldClock(Env env) {
        InstanceContainer lobby = PlatformBeans.createLobbyInstance(env.process(), env.process().instance(), Optional.of(LobbyAtmosphere.SLENDER_FOG));

        Assertions.assertEquals(env.process().dimensionType().get(DimensionType.OVERWORLD).defaultClock(), lobby.getCachedDimensionType().defaultClock(), "the fog dimension must run on the overworld clock so daytime still works");
    }

    @DisplayName("A season without an atmosphere keeps the overworld")
    @Test
    void noAtmosphereKeepsTheOverworld(Env env) {
        InstanceContainer lobby = PlatformBeans.createLobbyInstance(env.process(), env.process().instance(), Optional.empty());

        Assertions.assertEquals(DimensionType.OVERWORLD, lobby.getDimensionType(), "without an atmosphere the lobby must use the overworld");
    }
}
