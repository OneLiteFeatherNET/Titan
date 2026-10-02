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

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * Translated texts of the Jump &amp; Run. Rendering is explicit via {@link GlobalTranslator}, so it
 * does not
 * depend on Minestom's automatic component translation.
 */
final class RunMessages implements AutoCloseable {

    private static final String BUNDLE = "titan.jumprun.messages";
    private static final String NO_SPACE = "titan.jumprun.start.no_space";
    private static final String SCORE_ACTIONBAR = "titan.jumprun.score.actionbar";
    private static final String END_SCORE = "titan.jumprun.end.score";
    private static final String END_RECORD = "titan.jumprun.end.record";

    /** Every key this class renders; the bundle test checks them against the property files. */
    static final List<String> KEYS = List.of(NO_SPACE, SCORE_ACTIONBAR, END_SCORE, END_RECORD);

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "jumprun"));
    private boolean registered;

    RunMessages() {
        store.defaultLocale(Locale.ENGLISH);
        store.registerAll(Locale.ENGLISH, ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH), false);
        store.registerAll(Locale.GERMAN, ResourceBundle.getBundle(BUNDLE, Locale.GERMAN), false);
    }

    /** Adds the store to the {@link GlobalTranslator}; idempotent. */
    void register() {
        if (!registered) {
            registered = GlobalTranslator.translator().addSource(store);
        }
    }

    /** Removes the store again; idempotent. */
    @Override
    public void close() {
        if (registered) {
            GlobalTranslator.translator().removeSource(store);
            registered = false;
        }
    }

    Component noSpace(Locale locale) {
        return render(Component.translatable(NO_SPACE), locale);
    }

    Component scoreActionBar(Locale locale, int score) {
        return render(scored(SCORE_ACTIONBAR, score), locale);
    }

    Component endScore(Locale locale, int score) {
        return render(scored(END_SCORE, score), locale);
    }

    Component endRecord(Locale locale, int score) {
        return render(scored(END_RECORD, score), locale);
    }

    private static Component scored(String key, int score) {
        return Component.translatable(key, Argument.numeric("score", score));
    }

    private static Component render(Component translatable, Locale locale) {
        return GlobalTranslator.render(translatable, locale);
    }
}
