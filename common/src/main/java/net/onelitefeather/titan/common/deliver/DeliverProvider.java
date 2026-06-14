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
package net.onelitefeather.titan.common.deliver;

import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.utils.CloudNetEnvironment;

/**
 * Picks the {@link Deliver} implementation based on whether the server runs as a
 * CloudNet service. CloudNet is no longer bundled into the fat jar (it is provided
 * by the CloudNet wrapper at runtime), so standalone runs fall back to a no-op.
 */
public final class DeliverProvider {

    private DeliverProvider() {
    }

    public static Deliver create() {
        if (CloudNetEnvironment.isPresent()) {
            return new MessageChannelDeliver();
        }
        return new NoopDeliver();
    }
}
