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

import java.util.random.RandomGenerator;

/** Maps the score to how hard the next jump should be. */
final class Difficulty {

    /** Score at which the level has climbed to 1 - 1/e; a larger value flattens the curve. */
    private static final double SCALE = 40.0;
    private static final double NOISE_SIGMA = 1.0;

    private Difficulty() {
    }

    /** Smooth, rising level in [0, 1) that approaches 1 without a hand-set cap. */
    static double level(int score) {
        return 1.0 - Math.exp(-score / SCALE);
    }

    /**
     * The cost the next jump should come close to: the level scaled to the hardest jump, plus
     * noise.
     */
    static double targetCost(int score, RandomGenerator random) {
        double target = level(score) * Jump.MAX_COST + random.nextGaussian() * NOISE_SIGMA;
        return Math.clamp(target, 0.0, Jump.MAX_COST);
    }
}
