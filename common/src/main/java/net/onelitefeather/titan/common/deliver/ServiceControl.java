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

import java.util.List;
import java.util.Map;

/**
 * Starts and stops CloudNet services of on-demand tasks. Implemented in the CloudNet bridge
 * extension realm and invoked from the application via {@link TitanServiceControl}. Only JDK
 * types cross the classloader boundary.
 *
 * <p>A service row is a {@code Map<String, Object>} with {@link #NAME} (a {@code String}),
 * {@link #PHASE} (the name of the service phase, {@code STARTING}, {@code RUNNING} or
 * {@code STOPPED}), and {@link #ONLINE} and {@link #MAX} (each an {@code Integer}).
 */
public interface ServiceControl {

    /** Row key of the service name, a {@code String}. */
    String NAME = "name";

    /** Row key of the phase name, a {@code String}. */
    String PHASE = "phase";

    /** Row key of the players on the service, an {@code Integer}. */
    String ONLINE = "online";

    /** Row key of the player limit, an {@code Integer}; {@code 0} while none is announced. */
    String MAX = "max";

    /** The services of the task, one row each, starting ones included; throws when unreadable. */
    List<Map<String, Object>> servicesByTask(String task);

    /** Requests one new service of the task; {@code true} when the request was sent. */
    boolean start(String task);

    /** Requests the named service to stop; {@code true} when the request was sent. */
    boolean stop(String serviceName);
}
