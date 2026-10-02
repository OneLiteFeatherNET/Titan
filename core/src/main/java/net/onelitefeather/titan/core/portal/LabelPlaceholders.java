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
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * The placeholders of a label text, shared by validation and rendering so that a text the
 * validator accepts never contains a tag the renderer does not know.
 */
public final class LabelPlaceholders {

    /**
     * Lenient on purpose: strict mode would reject a text that leaves its last tag open, which is
     * normal for a label. {@link PortalValidator} finds unknown and mismatched tags itself.
     */
    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private LabelPlaceholders() {
    }

    /**
     * {@code <online>}, {@code <max>} and {@code <task>}; unparsed, so values never act as markup.
     */
    public static TagResolver counts(String online, String max, String task) {
        return TagResolver.resolver(
                plain("online", online), plain("max", max), plain("task", task));
    }

    /** All placeholders with sample values, for checking a text without a live source. */
    public static TagResolver samples() {
        return TagResolver.resolver(counts("0", "0", "task"), plain("prefix", "prefix"));
    }

    /**
     * Like {@code Placeholder.unparsed}, but rejects an argument such as {@code <online:group:x>}.
     */
    private static TagResolver plain(String name, String value) {
        return TagResolver.resolver(name, (arguments, context) -> {
            if (arguments.hasNext()) {
                throw context.newException("<" + name + "> takes no arguments", arguments);
            }
            return Tag.inserting(Component.text(value));
        });
    }
}
