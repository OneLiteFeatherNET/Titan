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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * The static dance lists the quick action can show: one per language and per combination of
 * permission-gated dances. The client cannot translate our keys and the quick-action tag differs
 * per player, so every list is registered once at startup and each player is pointed at his own.
 */
final class DanceList {

    static final Locale GERMAN = Locale.GERMAN;
    static final Locale ENGLISH = Locale.ENGLISH;

    /** The dances that need a permission; their combinations make the lists. */
    private static final List<Dance> GATED = Arrays.stream(Dance.values()).filter(dance -> dance.permission().isPresent()).toList();

    private DanceList() {
    }

    /**
     * @return the language the list is written in: German for German, English for everything else
     */
    static Locale languageOf(Locale locale) {
        return GERMAN.getLanguage().equals(locale.getLanguage()) ? GERMAN : ENGLISH;
    }

    /** @return every language a list exists in */
    static List<Locale> languages() {
        return List.of(GERMAN, ENGLISH);
    }

    /**
     * @return every set of dances a list can offer: the free dances, plus each combination of the
     *         gated ones; with one gated dance that is two sets, so the lists stay bounded
     */
    static List<Set<Dance>> variants() {
        List<Set<Dance>> variants = new ArrayList<>();
        for (int mask = 0; mask < (1 << GATED.size()); mask++) {
            EnumSet<Dance> permitted = EnumSet.noneOf(Dance.class);
            for (Dance dance : Dance.values()) {
                int gatedIndex = GATED.indexOf(dance);
                if (gatedIndex < 0 || (mask & (1 << gatedIndex)) != 0) {
                    permitted.add(dance);
                }
            }
            variants.add(Collections.unmodifiableSet(permitted));
        }
        return List.copyOf(variants);
    }

    /**
     * @param language  a language from {@link #languages()}
     * @param permitted the dances the list offers; only its gated dances change the key
     * @return the key of the list, for example {@code titan:dances/de} or
     *         {@code titan:dances/en_spin}
     */
    static Key key(Locale language, Set<Dance> permitted) {
        StringBuilder path = new StringBuilder("dances/").append(language.getLanguage());
        for (Dance dance : GATED) {
            if (permitted.contains(dance)) {
                path.append('_').append(dance.id());
            }
        }
        return Key.key("titan", path.toString());
    }
}
