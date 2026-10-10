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
package net.onelitefeather.titan.platform.luckperms;

import java.util.List;
import net.minestom.server.Auth;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LuckPermsPermissionServiceStartTest {

    private static ServerBootstrap bootstrapLoading(List<String> extensions) {
        return new ServerBootstrap() {
            @Override
            public String name() {
                return "extensions";
            }

            @Override
            public void init(Auth auth) {
            }

            @Override
            public void start(String host, int port) {
            }

            @Override
            public List<String> loadedExtensions() {
                return extensions;
            }
        };
    }

    @DisplayName("A LuckPerms extension reported by the server bootstrap aborts the start before LuckPerms is loaded")
    @Test
    void luckPermsExtensionFromBootstrapAbortsStart() {
        LuckPermsPermissionService service = new LuckPermsPermissionService(Telemetry.noop(), bootstrapLoading(List.of("LuckPerms")));

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, service::start);

        Assertions.assertTrue(thrown.getMessage().contains("loaded twice"), "the message must name the double load, was: " + thrown.getMessage());
    }
}
