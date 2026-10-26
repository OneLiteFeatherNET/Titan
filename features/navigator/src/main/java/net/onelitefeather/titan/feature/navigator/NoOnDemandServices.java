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

import java.util.List;
import java.util.Optional;
import net.onelitefeather.titan.core.service.OnDemandServices;
import net.onelitefeather.titan.core.service.ServiceState;

/**
 * The on-demand services when no provider is installed, as outside CloudNet: nothing can be read
 * and every request is refused, so a staging click is refused rather than started.
 */
final class NoOnDemandServices implements OnDemandServices {

    static final OnDemandServices INSTANCE = new NoOnDemandServices();

    private NoOnDemandServices() {
    }

    @Override
    public Optional<List<ServiceState>> services(String task) {
        return Optional.empty();
    }

    @Override
    public boolean start(String task) {
        return false;
    }

    @Override
    public boolean stop(String serviceName) {
        return false;
    }
}
