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

/**
 * A stored player value: a switch ({@link Flag}) or one level of a {@link LevelSetting}. Both are
 * kept under the same key format, so the store does not need to know which one it holds.
 */
public sealed interface SettingValue permits SettingValue.Flag, SettingValue.Level {

    /** The state of a switch. */
    record Flag(boolean enabled) implements SettingValue {
    }

    /** One whole-number level of a {@link LevelSetting}. */
    record Level(int value) implements SettingValue {
    }
}
