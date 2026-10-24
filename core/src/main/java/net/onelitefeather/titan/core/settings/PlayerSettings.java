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
package net.onelitefeather.titan.core.settings;

import java.util.UUID;

/** The player options of this lobby. Reads never fail; storage is an implementation detail. */
public interface PlayerSettings {

    /**
     * @return the player's value, or the setting's default while nothing is loaded or stored for
     *         the player
     */
    boolean isEnabled(UUID player, BooleanSetting setting);

    /** Switches the value at once; saving happens later, in the background. */
    void set(UUID player, BooleanSetting setting, boolean enabled);

    /** Called on the tick thread whenever a player's effective value changes. */
    void addChangeListener(SettingChangeListener listener);
}
