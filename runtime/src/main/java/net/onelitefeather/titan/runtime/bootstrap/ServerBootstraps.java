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

import java.util.List;
import java.util.stream.Collectors;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;

/** Chooses the one {@link ServerBootstrap} a variant starts its server with. */
public final class ServerBootstraps {

    private ServerBootstraps() {
    }

    /**
     * @return the plain Minestom bootstrap for no candidate, the candidate itself for exactly one
     * @throws IllegalStateException naming every candidate when there is more than one
     */
    public static ServerBootstrap select(List<ServerBootstrap> candidates) {
        if (candidates.isEmpty()) {
            return new PlainMinestomBootstrap();
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        String names = candidates.stream().map(ServerBootstrap::name).collect(Collectors.joining(", "));
        throw new IllegalStateException("More than one server bootstrap found: " + names);
    }
}
