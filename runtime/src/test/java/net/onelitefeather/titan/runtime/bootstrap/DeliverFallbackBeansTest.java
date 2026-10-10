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

import net.onelitefeather.titan.common.deliver.TracedDeliver;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliverFallbackBeansTest {

    @DisplayName("The fallback deliver is traced, so a transfer without a platform still shows in the trace")
    @Test
    void fallbackDeliverIsTraced() {
        Assertions.assertInstanceOf(TracedDeliver.class, new DeliverFallbackBeans().deliver(Telemetry.noop()));
    }
}
