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
package net.onelitefeather.titan.feature.radio;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import java.util.random.RandomGenerator;

/** The random source of the radio's mode choices; the tests pass a seeded one instead. */
@Factory
final class RadioBeans {

    @Bean
    RandomGenerator radioRandom() {
        return RandomGenerator.getDefault();
    }
}
