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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.List;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;

/** Lists the services it was given and counts how often it was asked. */
final class FakePlayerCounts implements PlayerCounts {

    private List<ServiceCount> services;
    private RuntimeException failure;
    private int reads;

    FakePlayerCounts(ServiceCount... services) {
        this.services = List.of(services);
    }

    /** What the following reads return. */
    void serve(ServiceCount... services) {
        this.services = List.of(services);
        this.failure = null;
    }

    /** Makes the following reads throw. */
    void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    int reads() {
        return this.reads;
    }

    @Override
    public PlayerCount count(SourceType type, String name) {
        throw new UnsupportedOperationException("the switcher lists services, it does not sum them");
    }

    @Override
    public List<ServiceCount> running(SourceType type, String name) {
        this.reads++;
        if (this.failure != null) {
            throw this.failure;
        }
        return this.services;
    }
}
