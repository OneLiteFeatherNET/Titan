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
package net.onelitefeather.titan.feature.jumprun;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A setting read from the config: strictly once at start, then whenever a run starts, so an edited
 * file takes effect without a restart. An invalid live value keeps the last valid one.
 */
final class LiveSetting<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveSetting.class);

    private final String name;
    private final Supplier<T> parser;
    private final Set<String> warned = new HashSet<>();
    private T lastValid;

    /**
     * @param parser reads and validates the value; it throws {@link IllegalArgumentException}
     *               naming the key and the reason
     */
    LiveSetting(String name, Supplier<T> parser) {
        this.name = name;
        this.parser = parser;
    }

    /**
     * @throws IllegalArgumentException naming the invalid key and the reason, which aborts the
     *                                  start
     */
    synchronized T readAtStartup() {
        this.lastValid = this.parser.get();
        return this.lastValid;
    }

    /**
     * The current value, or the last valid one while the config is invalid; each problem is warned
     * about once.
     */
    synchronized T current() {
        try {
            this.lastValid = this.parser.get();
            this.warned.clear();
        } catch (IllegalArgumentException e) {
            if (this.lastValid == null) {
                throw e;
            }
            if (this.warned.add(e.getMessage())) {
                LOGGER.warn("Invalid jumprun {}, keeping the last valid one: {}", this.name, e.getMessage());
            }
        }
        return this.lastValid;
    }
}
