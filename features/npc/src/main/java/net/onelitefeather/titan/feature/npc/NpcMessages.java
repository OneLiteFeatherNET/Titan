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
package net.onelitefeather.titan.feature.npc;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * Translated texts of the NPC and ambience modules. Every text is a translation key in the bundle,
 * rendered in the language of the player who sees it. The bundle test checks both property files
 * against {@link #KEYS}.
 */
@Singleton
final class NpcMessages implements AutoCloseable {

    static final String JOIN_NONE_READY = "titan.npc.join.none_ready";
    static final String JOIN_UNAVAILABLE = "titan.npc.join.unavailable";
    static final String JOIN_DENIED = "titan.npc.join.denied";
    static final String SIGN_WATCHING = "titan.ambience.sign.watching";
    static final String SIGN_PAGES = "titan.ambience.sign.pages";

    /** Every key the modules render. */
    static final List<String> KEYS = List.of(JOIN_NONE_READY, JOIN_UNAVAILABLE, JOIN_DENIED, SIGN_WATCHING, SIGN_PAGES);

    /** The keys a map file may give a sign; any other key is skipped with a warning. */
    static final List<String> SIGN_KEYS = List.of(SIGN_WATCHING, SIGN_PAGES);

    /** The languages the texts exist in; a player with another language sees English. */
    static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN);

    private static final String BUNDLE = "titan.npc.messages";

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "npc"));
    private boolean registered;

    @PostConstruct
    void register() {
        for (Locale language : LANGUAGES) {
            this.store.registerAll(language, ResourceBundle.getBundle(BUNDLE, language), false);
        }
        if (!this.registered) {
            this.registered = GlobalTranslator.translator().addSource(this.store);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (this.registered) {
            GlobalTranslator.translator().removeSource(this.store);
            this.registered = false;
        }
    }

    /** The text of a key in a language. */
    static Component text(String key, Locale locale) {
        return GlobalTranslator.render(Component.translatable(key), locale);
    }

    /** The language a player sees: German for a German client, English for everyone else. */
    static Locale languageOf(Locale playerLocale) {
        return playerLocale != null && Locale.GERMAN.getLanguage().equals(playerLocale.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH;
    }
}
