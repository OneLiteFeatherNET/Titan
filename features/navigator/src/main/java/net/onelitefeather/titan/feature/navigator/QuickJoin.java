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
package net.onelitefeather.titan.feature.navigator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * The service a left click on a minigame joins: a ready, not full one with the most players, so a
 * round fills before a new one starts. A tie goes to the lowest name, which keeps the choice the
 * same for the same snapshot.
 */
final class QuickJoin {

    private static final Comparator<ServiceCount> FULLEST_THEN_NAME = Comparator.comparingInt(ServiceCount::online).reversed().thenComparing(ServiceCount::name);

    private QuickJoin() {
    }

    /** The service to join, or nothing when no running service is ready and not full. */
    static Optional<ServiceCount> pick(List<ServiceCount> running) {
        return running.stream().filter(ServerPicker::joinable).min(FULLEST_THEN_NAME);
    }
}
