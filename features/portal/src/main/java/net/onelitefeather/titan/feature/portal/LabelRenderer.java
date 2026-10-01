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
package net.onelitefeather.titan.feature.portal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.onelitefeather.titan.core.portal.LabelPlaceholders;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;

/** Turns a label's MiniMessage text and a reading into the component the display shows. */
final class LabelRenderer {

    private static final String UNKNOWN_MAX = "?";

    private LabelRenderer() {
    }

    static Component render(Portal portal, PortalLabel label, LabelReading reading) {
        return render(LabelPlaceholders.MINI_MESSAGE, portal, label, reading);
    }

    /** The parser is a parameter so a test can bring its own {@code <prefix>}. */
    static Component render(MiniMessage parser, Portal portal, PortalLabel label, LabelReading reading) {
        String text = label.text();
        String online;
        String max;
        switch (reading) {
            case LabelReading.Local local -> {
                online = String.valueOf(local.online());
                max = UNKNOWN_MAX;
            }
            case LabelReading.Remote remote when remote.count().running() -> {
                online = String.valueOf(remote.count().online());
                max = String.valueOf(remote.count().max());
            }
            case LabelReading.Remote ignored -> {
                // A source that is not running shows zeros, whatever the provider reported for it.
                text = label.offlineText() != null ? label.offlineText() : label.text();
                online = "0";
                max = "0";
            }
        }
        return parser.deserialize(text, LabelPlaceholders.counts(online, max, portal.task()));
    }
}
