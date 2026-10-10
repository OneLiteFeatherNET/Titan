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
package net.onelitefeather.titan.platform.cloudnet;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.Primary;
import java.nio.file.Path;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.deliver.TracedDeliver;
import net.onelitefeather.titan.core.telemetry.Telemetry;

/**
 * The CloudNet deliver. {@link Primary} so it wins over the runtime's debug fallback, and it keeps
 * the transfer trace every delivery gets.
 */
@Factory
public final class CloudNetDeliverBeans {

    @Bean
    @Primary
    public Deliver deliver(Telemetry telemetry) {
        return new TracedDeliver(CloudNetDeliverSelection.select(Path.of("")), telemetry);
    }
}
