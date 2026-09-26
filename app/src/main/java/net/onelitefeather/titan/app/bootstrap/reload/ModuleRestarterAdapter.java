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
package net.onelitefeather.titan.app.bootstrap.reload;

import java.util.List;
import java.util.Objects;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import net.onelitefeather.titan.app.module.RestartOutcome;

/**
 * Production {@link ModuleRestarter}: delegates to {@link ModuleRegistry#restart(String)} and maps
 * its own sealed {@link RestartOutcome} to this package's {@link ModuleRestartOutcome} - see that
 * type's javadoc for why {@link ConfigChangeHandler} does not depend on the {@code module} package
 * directly. {@link #moduleOrder()} is backed the same way, by {@link ModuleRegistry#moduleIds()}.
 */
final class ModuleRestarterAdapter implements ModuleRestarter {

    private final ModuleRegistry moduleRegistry;

    ModuleRestarterAdapter(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = Objects.requireNonNull(moduleRegistry, "moduleRegistry");
    }

    @Override
    public ModuleRestartOutcome restart(String moduleId) {
        return switch (this.moduleRegistry.restart(moduleId)) {
            case RestartOutcome.Restarted restarted -> new ModuleRestartOutcome.Restarted();
            case RestartOutcome.Failed(Throwable cause) -> new ModuleRestartOutcome.Failed(cause);
        };
    }

    @Override
    public List<String> moduleOrder() {
        return this.moduleRegistry.moduleIds();
    }
}
