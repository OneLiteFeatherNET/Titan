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

/** Which value a setting has until a player chooses otherwise. */
public enum SettingDefault {

    /** Off until the player switches it on, e.g. joining a game that needs consent. */
    OPT_IN(false),

    /** On until the player switches it off, e.g. being a target the player may refuse. */
    OPT_OUT(true);

    private final boolean initialValue;

    SettingDefault(boolean initialValue) {
        this.initialValue = initialValue;
    }

    public boolean initialValue() {
        return this.initialValue;
    }
}
