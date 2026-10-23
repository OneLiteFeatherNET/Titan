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

import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AmbienceValidatorTest {

    private static final Box AREA = new Box(new Vec(82, 78, 152), new Vec(123, 90, 163));

    private static AmbienceZone zone(Box area, AmbienceZone.Particles particles, AmbienceZone.Sounds sounds, List<AmbienceZone.Sign> signs, List<Vec> apparitions) {
        return new AmbienceZone("slender-tunnel", area, particles, sounds, signs, List.of(), apparitions, null);
    }

    @DisplayName("A zone with vanilla particles, a sound and points has no problems")
    @Test
    void validZoneHasNoProblems() {
        AmbienceZone zone = zone(AREA, new AmbienceZone.Particles("white_ash", 3), new AmbienceZone.Sounds("minecraft:ambient.cave", 20, 60), List.of(new AmbienceZone.Sign(new Vec(98, 82, 152), "titan.ambience.sign.watching")), List.of(new Vec(112, 80, 157)));
        Assertions.assertEquals(List.of(), AmbienceValidator.problems(zone), "a complete zone is usable, with or without the namespace");
    }

    @DisplayName("A zone without an area is a problem")
    @Test
    void missingAreaIsReported() {
        Assertions.assertEquals(List.of("area is missing"), AmbienceValidator.problems(zone(null, null, null, List.of(), List.of())), "a zone must cover a box");
    }

    @DisplayName("An inverted area names the axis")
    @Test
    void invertedAxisIsReported() {
        Box inverted = new Box(new Vec(123, 78, 152), new Vec(82, 90, 163));
        Assertions.assertEquals(List.of("area min.x (123.0) is greater than max.x (82.0)"), AmbienceValidator.problems(zone(inverted, null, null, List.of(), List.of())), "min above max is a bad box");
    }

    @DisplayName("A particle that is not vanilla is reported by name")
    @Test
    void customParticleIsReported() {
        List<String> problems = AmbienceValidator.problems(zone(AREA, new AmbienceZone.Particles("slender_dust", 2), null, List.of(), List.of()));
        Assertions.assertEquals(1, problems.size(), "one problem for the particle: " + problems);
        Assertions.assertTrue(problems.getFirst().contains("slender_dust"), "the problem names the particle: " + problems);
    }

    @DisplayName("A sound that is not vanilla is reported by name")
    @Test
    void customSoundIsReported() {
        List<String> problems = AmbienceValidator.problems(zone(AREA, null, new AmbienceZone.Sounds("titan.slender.whisper", 10, 20), List.of(), List.of()));
        Assertions.assertTrue(problems.stream().anyMatch(problem -> problem.contains("titan.slender.whisper")), "the problem names the sound: " + problems);
    }

    @DisplayName("A particle count outside 1 to 20 is a problem")
    @Test
    void particleCountOutOfRangeIsReported() {
        Assertions.assertFalse(AmbienceValidator.problems(zone(AREA, new AmbienceZone.Particles("ash", 0), null, List.of(), List.of())).isEmpty(), "zero particles is no zone at all");
    }

    @DisplayName("A sound interval with minimum above maximum is a problem")
    @Test
    void reversedSoundIntervalIsReported() {
        Assertions.assertFalse(AmbienceValidator.problems(zone(AREA, null, new AmbienceZone.Sounds("ambient.cave", 60, 20), List.of(), List.of())).isEmpty(), "the interval must run forwards");
    }

    @DisplayName("A sign without a key or a page without a position is a problem")
    @Test
    void incompleteSignIsReported() {
        Assertions.assertEquals(List.of("sign.key must not be blank"), AmbienceValidator.problems(zone(AREA, null, null, List.of(new AmbienceZone.Sign(new Vec(1, 1, 1), " ")), List.of())), "a sign needs its text key");
    }
}
