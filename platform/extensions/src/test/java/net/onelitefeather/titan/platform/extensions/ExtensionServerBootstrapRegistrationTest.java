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
package net.onelitefeather.titan.platform.extensions;

import java.util.ServiceLoader;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtensionServerBootstrapRegistrationTest {

    @DisplayName("The extension bootstrap is registered for ServiceLoader under the name extensions")
    @Test
    void extensionBootstrapIsDiscoverableByServiceLoader() {
        ServerBootstrap found = ServiceLoader.load(ServerBootstrap.class).stream().map(ServiceLoader.Provider::get).filter(ExtensionServerBootstrap.class::isInstance).findFirst().orElseThrow(() -> new AssertionError("ExtensionServerBootstrap is missing from META-INF/services"));

        Assertions.assertEquals("extensions", found.name());
    }
}
