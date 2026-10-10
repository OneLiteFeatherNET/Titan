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
 * Reads player counts from CloudNet. Implemented in the CloudNet bridge extension realm and
 * invoked from the application via {@link TitanPlayerCountLookup}. Only JDK types cross the
 * classloader boundary; {@code type} is {@code task}, {@code group} or {@code service}.
 *
 * <p>A running service is a {@code Map<String, Object>} with the keys {@link #NAME} (a
 * {@code String}) and {@link #ONLINE} and {@link #MAX} (each an {@code Integer}); the application
 * maps the rows to its own types.
 */
public interface PlayerCountLookup {

    /** Row key of the service name, a {@code String}. */
    String NAME = "name";

    /** Row key of the players on the service, an {@code Integer}. */
    String ONLINE = "online";

    /** Row key of the player limit, an {@code Integer}; {@code 0} while none is announced. */
    String MAX = "max";

    boolean supports(String type);

    /** The running services of that source, one row each (see above); empty when none runs. */
    List<Map<String, Object>> running(String type, String name);
}
