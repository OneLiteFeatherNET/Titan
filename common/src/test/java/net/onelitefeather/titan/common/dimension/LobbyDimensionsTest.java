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

import net.minestom.server.color.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Re-derives the slender fog from its two sources, the way Cygnus does it: the BRIGHT preset and
 * the Granskoga map atmosphere, mixed with share 0.3f. The constants in {@link LobbyDimensions}
 * must equal that derivation, so a changed source value shows up here.
 */
class LobbyDimensionsTest {

    private static final float SHARE = 0.3f;
    private static final float AMBIENT_SHARE = 0.10f;

    // Cygnus StaticDimensionPreset.BRIGHT
    private static final Color BRIGHT_FOG = new Color(55, 175, 170);
    private static final Color BRIGHT_SKY_LIGHT = new Color(100, 220, 220);
    private static final Color BRIGHT_SKY = new Color(10, 35, 45);
    private static final float BRIGHT_SKY_LIGHT_FACTOR = 0.05f;
    private static final float BRIGHT_FOG_START = 32f;
    private static final float BRIGHT_FOG_END = 256f;
    private static final float BRIGHT_SKY_FOG_END = 160f;

    // Cygnus game/game/maps/Granskoga/map.json; no ambientLightColor, so it is dim(fog, 0.10)
    private static final Color GRANSKOGA_FOG = new Color(0x0F, 0x15, 0x12);
    private static final Color GRANSKOGA_SKY_LIGHT = new Color(0x1A, 0x24, 0x20);
    private static final Color GRANSKOGA_SKY = new Color(0x05, 0x07, 0x06);
    private static final float GRANSKOGA_SKY_LIGHT_FACTOR = 0.004f;
    private static final float GRANSKOGA_FOG_START = 0f;
    private static final float GRANSKOGA_FOG_END = 34f;
    private static final float GRANSKOGA_SKY_FOG_END = 26f;

    private static final float DISTANCE_TOLERANCE = 1.0E-4f;

    private static float lerp(float open, float target) {
        return open + (target - open) * SHARE;
    }

    private static int mix(int open, int target) {
        return Math.round(lerp(open, target));
    }

    private static Color mix(Color open, Color target) {
        return new Color(mix(open.red(), target.red()), mix(open.green(), target.green()), mix(open.blue(), target.blue()));
    }

    private static Color dim(Color color, float factor) {
        return new Color(
                Math.max(1, Math.round(color.red() * factor)), Math.max(1, Math.round(color.green() * factor)), Math.max(1, Math.round(color.blue() * factor))
        );
    }

    private static void assertColor(Color expected, Color actual, String name) {
        Assertions.assertEquals(expected.red(), actual.red(), "the red channel of " + name + " must match the derivation");
        Assertions.assertEquals(expected.green(), actual.green(), "the green channel of " + name + " must match the derivation");
        Assertions.assertEquals(expected.blue(), actual.blue(), "the blue channel of " + name + " must match the derivation");
    }

    @DisplayName("The fog colour is the BRIGHT fog mixed 30 % towards the Granskoga fog")
    @Test
    void fogColorIsDerived() {
        assertColor(mix(BRIGHT_FOG, GRANSKOGA_FOG), LobbyDimensions.FOG_COLOR, "the fog colour");
    }

    @DisplayName("The sky light colour is the BRIGHT sky light mixed 30 % towards the Granskoga sky light")
    @Test
    void skyLightColorIsDerived() {
        assertColor(mix(BRIGHT_SKY_LIGHT, GRANSKOGA_SKY_LIGHT), LobbyDimensions.SKY_LIGHT_COLOR, "the sky light colour");
    }

    @DisplayName("The sky colour is the BRIGHT sky mixed 30 % towards the Granskoga sky")
    @Test
    void skyColorIsDerived() {
        assertColor(mix(BRIGHT_SKY, GRANSKOGA_SKY), LobbyDimensions.SKY_COLOR, "the sky colour");
    }

    @DisplayName("The ambient colour mixes the two ambient colours, each the dimmed fog of its source")
    @Test
    void ambientLightColorIsDerived() {
        Color brightAmbient = dim(BRIGHT_FOG, AMBIENT_SHARE);
        Color granskogaAmbient = dim(GRANSKOGA_FOG, AMBIENT_SHARE);

        assertColor(mix(brightAmbient, granskogaAmbient), LobbyDimensions.AMBIENT_LIGHT_COLOR, "the ambient light colour");
    }

    @DisplayName("The sky light factor is the BRIGHT factor mixed 30 % towards the Granskoga factor")
    @Test
    void skyLightFactorIsDerived() {
        Assertions.assertEquals(lerp(BRIGHT_SKY_LIGHT_FACTOR, GRANSKOGA_SKY_LIGHT_FACTOR), LobbyDimensions.SKY_LIGHT_FACTOR, DISTANCE_TOLERANCE, "the sky light factor must match the derivation");
    }

    @DisplayName("The fog start distance is the BRIGHT start mixed 30 % towards the Granskoga start")
    @Test
    void fogStartDistanceIsDerived() {
        Assertions.assertEquals(lerp(BRIGHT_FOG_START, GRANSKOGA_FOG_START), LobbyDimensions.FOG_START_DISTANCE, DISTANCE_TOLERANCE, "the fog start must match the derivation");
    }

    @DisplayName("The fog end distance is the BRIGHT end mixed 30 % towards the Granskoga end")
    @Test
    void fogEndDistanceIsDerived() {
        Assertions.assertEquals(lerp(BRIGHT_FOG_END, GRANSKOGA_FOG_END), LobbyDimensions.FOG_END_DISTANCE, DISTANCE_TOLERANCE, "the fog end must match the derivation");
    }

    @DisplayName("The sky fog end distance is the BRIGHT sky fog end mixed 30 % towards the Granskoga one")
    @Test
    void skyFogEndDistanceIsDerived() {
        Assertions.assertEquals(lerp(BRIGHT_SKY_FOG_END, GRANSKOGA_SKY_FOG_END), LobbyDimensions.SKY_FOG_END_DISTANCE, DISTANCE_TOLERANCE, "the sky fog end must match the derivation");
    }
}
