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
package net.onelitefeather.titan.runtime.variant;

import io.avaje.inject.spi.AvajeModule;
import io.avaje.inject.spi.Builder;

/**
 * A minimal, registered (see {@code META-INF/services/io.avaje.inject.spi.InjectExtension} in
 * {@code src/test/resources}) {@link AvajeModule} fixture named like a future platform module's
 * generated module (e.g. {@code luckpermsPlatform}), so {@link LoadedModulesTest} proves discovery
 * is generic and not tied to the {@code Column} naming convention.
 */
public final class BarPlatformModule implements AvajeModule {

    @Override
    public Class<?>[] classes() {
        return new Class<?>[0];
    }

    @Override
    public void build(Builder builder) {
    }
}
