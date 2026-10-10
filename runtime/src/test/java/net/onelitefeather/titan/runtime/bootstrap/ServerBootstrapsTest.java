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
import net.minestom.server.Auth;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServerBootstrapsTest {

    @DisplayName("Without a bootstrap platform the plain Minestom bootstrap is chosen")
    @Test
    void noCandidateFallsBackToPlainMinestom() {
        ServerBootstrap chosen = ServerBootstraps.select(List.of());

        Assertions.assertEquals("minestom", chosen.name());
    }

    @DisplayName("A single bootstrap platform is the one chosen")
    @Test
    void oneCandidateIsChosen() {
        ServerBootstrap only = new NamedBootstrap("extensions");

        Assertions.assertSame(only, ServerBootstraps.select(List.of(only)));
    }

    @DisplayName("Two bootstrap platforms abort the start, naming both")
    @Test
    void twoCandidatesAbortNamingBoth() {
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> ServerBootstraps.select(List.of(new NamedBootstrap("extensions"), new NamedBootstrap("cloudnet"))));

        Assertions.assertTrue(thrown.getMessage().contains("extensions"), "the message must name extensions, was: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("cloudnet"), "the message must name cloudnet, was: " + thrown.getMessage());
    }

    private record NamedBootstrap(String name) implements ServerBootstrap {

        @Override
        public void init(Auth auth) {
        }

        @Override
        public void start(String host, int port) {
        }

        @Override
        public List<String> loadedExtensions() {
            return List.of();
        }
    }
}
