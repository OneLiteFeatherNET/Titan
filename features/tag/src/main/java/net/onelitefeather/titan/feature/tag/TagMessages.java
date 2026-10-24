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
package net.onelitefeather.titan.feature.tag;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;

/**
 * Translated texts of the tag column, in German and English. Each text is rendered by the {@link
 * GlobalTranslator}, so {@link #register()} must run before a text is shown.
 */
final class TagMessages implements AutoCloseable {

    static final String TITLE = "titan.tag.title";
    static final String SUBTITLE = "titan.tag.subtitle";
    static final String TAGGER = "titan.tag.actionbar.tagger";
    static final String WAITING = "titan.tag.actionbar.waiting";
    static final String COOLDOWN = "titan.tag.actionbar.cooldown";
    static final String TOGGLED_ON = "titan.tag.toggle.on";
    static final String TOGGLED_OFF = "titan.tag.toggle.off";
    static final String TOGGLE_UNAVAILABLE = "titan.tag.toggle.unavailable";

    /** Every key the column renders; the bundle test checks the property files against it. */
    static final List<String> KEYS = List.of(TITLE, SUBTITLE, TAGGER, WAITING, COOLDOWN, TOGGLED_ON, TOGGLED_OFF, TOGGLE_UNAVAILABLE);

    private static final String BUNDLE = "titan.tag.messages";

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "tag"));
    private boolean registered;

    TagMessages() {
        this.store.defaultLocale(Locale.ENGLISH);
        this.store.registerAll(Locale.ENGLISH, ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH), false);
        this.store.registerAll(Locale.GERMAN, ResourceBundle.getBundle(BUNDLE, Locale.GERMAN), false);
    }

    /** Adds the store to the {@link GlobalTranslator}; idempotent. */
    void register() {
        if (!this.registered) {
            this.registered = GlobalTranslator.translator().addSource(this.store);
        }
    }

    /** Removes the store again; idempotent. */
    @Override
    public void close() {
        if (this.registered) {
            GlobalTranslator.translator().removeSource(this.store);
            this.registered = false;
        }
    }

    /** @return the text under {@code key}, rendered for {@code locale} */
    static Component text(String key, Locale locale) {
        return GlobalTranslator.render(Component.translatable(key), locale);
    }
}
