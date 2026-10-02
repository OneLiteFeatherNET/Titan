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
package net.onelitefeather.titan.core.portal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Picks the text of a label and fills its placeholders. Lobby and setup preview both call it, so
 * what a builder sees is what players get.
 */
public final class LabelText {

    private LabelText() {
    }

    /**
     * The parser is a parameter so a test can bring its own {@code <prefix>}.
     *
     * @param offline whether the source is not running; shows {@code offlineText} when there is
     *                one,
     *                otherwise {@code text}
     */
    public static Component render(MiniMessage parser, PortalLabel label, String task, String online, String max, boolean offline) {
        String text = offline && label.offlineText() != null ? label.offlineText() : label.text();
        return parser.deserialize(text, LabelPlaceholders.counts(online, max, task));
    }

    /** Like the other overload, with the shared parser. */
    public static Component render(PortalLabel label, String task, String online, String max, boolean offline) {
        return render(LabelPlaceholders.MINI_MESSAGE, label, task, online, max, offline);
    }
}
