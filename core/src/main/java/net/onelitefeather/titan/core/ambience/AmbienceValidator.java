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

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import org.jetbrains.annotations.Nullable;

/**
 * The reasons an ambience zone is unusable. The lobby skips such a zone with a warning naming these
 * reasons; the other zones of the world still start.
 */
public final class AmbienceValidator {

    private static final int MAX_PARTICLE_COUNT = 20;

    private AmbienceValidator() {
    }

    /** Every reason the zone is unusable; empty if it is fine. */
    public static List<String> problems(AmbienceZone zone) {
        List<String> problems = new ArrayList<>();
        if (zone.id() == null || zone.id().isBlank()) {
            problems.add("id must not be blank");
        }
        areaProblems(zone.area(), problems);
        if (zone.particles() != null) {
            particleProblems(zone.particles(), problems);
        }
        if (zone.sounds() != null) {
            soundProblems(zone.sounds(), problems);
        }
        for (AmbienceZone.Sign sign : zone.signs()) {
            positionProblem("sign", sign.position(), problems);
            if (sign.key() == null || sign.key().isBlank()) {
                problems.add("sign.key must not be blank");
            }
        }
        zone.pages().forEach(page -> positionProblem("page", page, problems));
        zone.apparitions().forEach(point -> positionProblem("apparition", point, problems));
        if (zone.apparitionSkin() != null && (zone.apparitionSkin().value() == null || zone.apparitionSkin().value().isBlank())) {
            problems.add("apparitionSkin.value must not be blank");
        }
        return problems;
    }

    private static void areaProblems(@Nullable Box area, List<String> problems) {
        if (area == null) {
            problems.add("area is missing");
            return;
        }
        axis("x", area.min().x(), area.max().x(), problems);
        axis("y", area.min().y(), area.max().y(), problems);
        axis("z", area.min().z(), area.max().z(), problems);
    }

    private static void axis(String name, double min, double max, List<String> problems) {
        if (min > max) {
            problems.add("area min." + name + " (" + min + ") is greater than max." + name + " (" + max + ")");
        }
    }

    private static void particleProblems(AmbienceZone.Particles particles, List<String> problems) {
        if (particles.type() == null || !VanillaAmbience.isParticle(particles.type())) {
            problems.add("particles.type '" + particles.type() + "' is not a vanilla particle (expected " + String.join(", ", VanillaAmbience.PARTICLES) + ")");
        }
        if (particles.count() < 1 || particles.count() > MAX_PARTICLE_COUNT) {
            problems.add("particles.count must be from 1 to " + MAX_PARTICLE_COUNT + " but was " + particles.count());
        }
    }

    private static void soundProblems(AmbienceZone.Sounds sounds, List<String> problems) {
        if (sounds.key() == null || !VanillaAmbience.isSound(sounds.key())) {
            problems.add("sounds.key '" + sounds.key() + "' is not a vanilla sound (expected " + String.join(", ", VanillaAmbience.SOUNDS) + ")");
        }
        if (!(sounds.minSeconds() > 0) || !(sounds.maxSeconds() >= sounds.minSeconds())) {
            problems.add("sounds interval must be positive with minSeconds <= maxSeconds but was " + sounds.minSeconds() + ".." + sounds.maxSeconds());
        }
    }

    private static void positionProblem(String what, @Nullable Vec position, List<String> problems) {
        if (position == null) {
            problems.add(what + ".position is missing");
        }
    }
}
