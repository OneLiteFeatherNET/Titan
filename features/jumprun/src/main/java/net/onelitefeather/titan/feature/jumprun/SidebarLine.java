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

import java.util.Optional;
import net.kyori.adventure.text.Component;

/**
 * One line of the sidebar: the text on the left and, optionally, a value in the number column on
 * the right.
 */
record SidebarLine(Component text, Optional<Component> value) {

    static SidebarLine withValue(Component text, Component value) {
        return new SidebarLine(text, Optional.of(value));
    }

    static SidebarLine textOnly(Component text) {
        return new SidebarLine(text, Optional.empty());
    }
}
