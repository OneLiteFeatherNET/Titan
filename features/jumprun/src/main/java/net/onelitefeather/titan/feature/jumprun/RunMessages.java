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
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.onelitefeather.titan.feature.jumprun.course.Mode;

/**
 * Translated texts of the Jump &amp; Run. Rendering is explicit via {@link GlobalTranslator}, so it
 * does not depend on Minestom's automatic component translation.
 */
final class RunMessages implements AutoCloseable {

    private static final String BUNDLE = "titan.jumprun.messages";
    private static final String NO_SPACE = "titan.jumprun.start.no_space";
    private static final String SCORE_ACTIONBAR = "titan.jumprun.score.actionbar";
    private static final String END_SCORE = "titan.jumprun.end.score";
    private static final String END_RECORD = "titan.jumprun.end.record";
    private static final String MODE_CHANGED = "titan.jumprun.mode.changed";

    private static final String SIDEBAR_SCORE = "titan.jumprun.sidebar.score";
    private static final String SIDEBAR_SCORE_VALUE = "titan.jumprun.sidebar.score.value";
    private static final String SIDEBAR_RECORD = "titan.jumprun.sidebar.record";
    private static final String SIDEBAR_RECORD_VALUE = "titan.jumprun.sidebar.record.value";
    private static final String SIDEBAR_NO_RECORD = "titan.jumprun.sidebar.record.none";
    private static final String SIDEBAR_TOP_HEADER = "titan.jumprun.sidebar.top.header";
    private static final String SIDEBAR_TOP_ENTRY = "titan.jumprun.sidebar.top.entry";
    private static final String SIDEBAR_SELF_MARKER = "titan.jumprun.sidebar.self.marker";
    private static final String SIDEBAR_PLACE_VALUE = "titan.jumprun.sidebar.place.";
    private static final int PLACES = 3;

    /** Every key this class renders; the bundle test checks them against the property files. */
    static final List<String> KEYS = List.of(NO_SPACE, SCORE_ACTIONBAR, END_SCORE, END_RECORD, MODE_CHANGED, SIDEBAR_SCORE, SIDEBAR_SCORE_VALUE, SIDEBAR_RECORD, SIDEBAR_RECORD_VALUE, SIDEBAR_NO_RECORD, SIDEBAR_TOP_HEADER, SIDEBAR_TOP_ENTRY, SIDEBAR_SELF_MARKER, placeValueKey(1), placeValueKey(2), placeValueKey(3));

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

    Component endScore(Locale locale, Mode mode, int score) {
        return render(scored(END_SCORE, mode, score), locale);
    }

    Component endRecord(Locale locale, Mode mode, int score) {
        return render(scored(END_RECORD, mode, score), locale);
    }

    Component modeChanged(Locale locale, Mode mode) {
        return render(Component.translatable(MODE_CHANGED, modeArgument(mode)), locale);
    }

    Component sidebarScoreLabel(Locale locale) {
        return render(Component.translatable(SIDEBAR_SCORE), locale);
    }

    Component sidebarScoreValue(Locale locale, int score) {
        return render(scored(SIDEBAR_SCORE_VALUE, score), locale);
    }

    Component sidebarRecordLabel(Locale locale) {
        return render(Component.translatable(SIDEBAR_RECORD), locale);
    }

    Component sidebarRecordValue(Locale locale, int record) {
        return render(Component.translatable(SIDEBAR_RECORD_VALUE, Argument.numeric("record", record)), locale);
    }

    Component sidebarNoRecord(Locale locale) {
        return render(Component.translatable(SIDEBAR_NO_RECORD), locale);
    }

    Component sidebarTopHeader(Locale locale) {
        return render(Component.translatable(SIDEBAR_TOP_HEADER), locale);
    }

    Component sidebarSelfMarker(Locale locale) {
        return render(Component.translatable(SIDEBAR_SELF_MARKER), locale);
    }

    /**
     * @param marker the own-line marker, or any filler of the same width for the others
     * @param head   the player head object
     * @param name   inserted as given, never parsed as markup, so the caller decides its style
     */
    Component sidebarTopEntry(Locale locale, Component marker, Component head, Component name) {
        return render(Component.translatable(SIDEBAR_TOP_ENTRY, Argument.component("marker", marker), Argument.component("head", head), Argument.component("name", name)), locale);
    }

    /** @param place 1 to 3; each has its own medal colour in the bundle */
    Component sidebarPlaceValue(Locale locale, int place, int score) {
        return render(scored(placeValueKey(place), score), locale);
    }

    private static String placeValueKey(int place) {
        if (place < 1 || place > PLACES) {
            throw new IllegalArgumentException("place must be 1 to " + PLACES + " but was " + place);
        }
        return SIDEBAR_PLACE_VALUE + place + ".value";
    }

    private static ComponentLike modeArgument(Mode mode) {
        return Argument.component("mode", mode.label());
    }

    private static Component scored(String key, int score) {
        return Component.translatable(key, Argument.numeric("score", score));
    }

    private static Component scored(String key, Mode mode, int score) {
        return Component.translatable(key, Argument.numeric("score", score), modeArgument(mode));
    }

    private static Component render(Component translatable, Locale locale) {
        return GlobalTranslator.render(translatable, locale);
    }
}
