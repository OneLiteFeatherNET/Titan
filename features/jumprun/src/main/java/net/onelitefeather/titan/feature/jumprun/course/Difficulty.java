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
package net.onelitefeather.titan.feature.jumprun.course;

import java.util.random.RandomGenerator;

/** Maps the score to how hard the next jump should be. */
final class Difficulty {

    private static final double NOISE_SIGMA = 1.0;

    private Difficulty() {
    }

    /** Smooth, rising level in [0, 1) that approaches 1 without a hand-set cap. */
    static double level(Mode mode, int score) {
        return 1.0 - Math.exp(-score / mode.scale());
    }

    /**
     * The cost the next jump should come close to: the level scaled to the hardest jump of the
     * shapes unlocked at the score, plus noise. Scaling to all shapes would push the target into
     * wide gaps while the narrow shapes are still locked.
     */
    static double targetCost(Mode mode, int score, RandomGenerator random) {
        double hardest = Jump.maxCost(mode, mode.unlockedAt(score));
        double target = level(mode, score) * hardest + random.nextGaussian() * NOISE_SIGMA;
        return Math.clamp(target, 0.0, hardest);
    }
}
