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

import org.jetbrains.annotations.NotNull;

/**
 * One service of an on-demand task as the provider reports it.
 *
 * @param name   the service name, e.g. {@code Slender-Staging-1}
 * @param phase  where the service is in its life
 * @param online players currently on the service
 * @param max    the player limit; {@code 0} while the service has not announced one
 */
public record ServiceState(@NotNull String name, @NotNull Phase phase, int online, int max) {

    /** Whether the service is running and has announced a limit, the same rule as a quick join. */
    public boolean ready() {
        return this.phase == Phase.RUNNING && this.max > 0;
    }

    /** The life of a service as far as Titan needs it. */
    public enum Phase {
        /** Requested or preparing; not yet joinable. */
        STARTING,
        /** Running. */
        RUNNING,
        /** Stopped or being removed. */
        STOPPED
    }
}
