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
package net.onelitefeather.titan.common.config;

import java.util.Map;

/**
 * A stand-in for the real {@code navigator} module's config record, used to exercise {@link
 * ConfigSections}' binding of a {@code Map<String, Record>} section (see {@code design.md},
 * decision 3: navigator entries are keyed by name, e.g. {@code entries.survival.slot}) without
 * depending on the {@code app} module.
 */
record NavigatorTestConfig(String title, Map<String, Entry> entries) {

    static final NavigatorTestConfig DEFAULTS = new NavigatorTestConfig("Navigator", Map.of("survival", new Entry(1, "survival-lobby")));

    record Entry(int slot, String destination) {

        Entry {
            if (slot < 0) {
                throw ConfigException.invalid("slot", "must not be negative");
            }
        }
    }
}
