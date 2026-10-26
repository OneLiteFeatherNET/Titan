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
package net.onelitefeather.titan.core.service;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Starts and stops the services of on-demand tasks. A provider is installed by the platform;
 * without one, nothing can be read and every request is refused.
 */
public interface OnDemandServices {

    /**
     * The services of the named task, one entry each; empty when the task runs nowhere, and an
     * empty Optional when they cannot be read.
     */
    @NotNull
    Optional<List<ServiceState>> services(@NotNull String task);

    /** Asks for one new service of the named task; {@code true} when the request was accepted. */
    boolean start(@NotNull String task);

    /**
     * Asks for the named service to stop; idempotent, so a repeated request for a service that is
     * already stopping is refused and not an error. {@code true} when the request was accepted.
     */
    boolean stop(@NotNull String serviceName);
}
