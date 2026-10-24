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
package net.onelitefeather.titan.feature.throwing;

import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.SettingDefault;
import net.onelitefeather.titan.core.settings.SettingKey;

/** The player option that lets a player refuse being thrown. */
final class ThrowSettings {

    /** On by default: being thrown is opt-out. Switching it off is the player's refusal. */
    static final BooleanSetting TARGET = new BooleanSetting(SettingKey.parse("titan:throw.target"), SettingDefault.OPT_OUT);

    private ThrowSettings() {
    }
}
