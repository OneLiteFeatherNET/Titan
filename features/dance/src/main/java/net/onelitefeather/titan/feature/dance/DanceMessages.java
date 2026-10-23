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
package net.onelitefeather.titan.feature.dance;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;

/**
 * Translated texts of the dance column, in German and English. Each text is rendered by the
 * {@link GlobalTranslator}, so {@link #register()} must run before a text is shown.
 */
final class DanceMessages implements AutoCloseable {

    static final String TITLE = "titan.dance.title";
    static final String STOP = "titan.dance.stop";
    static final String BUSY = "titan.dance.busy";
    static final String COOLDOWN = "titan.dance.cooldown";

    /** Every key the column renders; the bundle test checks the property files against it. */
    static final List<String> KEYS = List.of(TITLE, STOP, BUSY, COOLDOWN, Dance.WAVE.labelKey(), Dance.GROOVE.labelKey(), Dance.PARTY.labelKey(), Dance.SPIN.labelKey());

    private static final String BUNDLE = "titan.dance.messages";

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "dance"));
    private boolean registered;

    DanceMessages() {
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
