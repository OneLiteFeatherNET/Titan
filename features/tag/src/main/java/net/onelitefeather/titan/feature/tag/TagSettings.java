/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.tag;

import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.SettingDefault;
import net.onelitefeather.titan.core.settings.SettingKey;

/**
 * The player's choice to take part in the tag game. Off by default: nobody is chased without
 * having asked to be.
 */
final class TagSettings {

    static final BooleanSetting ENABLED = new BooleanSetting(SettingKey.parse("titan:tag.enabled"), SettingDefault.OPT_IN);

    private TagSettings() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
