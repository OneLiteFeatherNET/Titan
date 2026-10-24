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
package net.onelitefeather.titan.feature.throwing;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.onelitefeather.titan.feature.throwing.ThrowRules.Refusal;

/** The texts of the throw column, in the bundles of {@code titan/throw}. */
final class ThrowMessages implements AutoCloseable {

    static final String HINT = "titan.throw.hint";
    static final String REFUSED_PERMISSION = "titan.throw.refused.permission";
    static final String REFUSED_TARGET = "titan.throw.refused.target";
    static final String REFUSED_STATE = "titan.throw.refused.state";
    static final String REFUSED_COOLDOWN = "titan.throw.refused.cooldown";

    /** Every key the column renders; the bundle test checks the property files against it. */
    static final List<String> KEYS = List.of(HINT, REFUSED_PERMISSION, REFUSED_TARGET, REFUSED_STATE, REFUSED_COOLDOWN);

    private static final String BUNDLE = "titan.throw.messages";

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "throw"));
    private boolean registered;

    ThrowMessages() {
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

    /** The language a player reads the texts in; any language without texts reads English. */
    static Locale languageOf(Locale locale) {
        return Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH;
    }

    /** @return the text under {@code key}, rendered for {@code locale} */
    static Component text(String key, Locale locale) {
        return GlobalTranslator.render(Component.translatable(key), languageOf(locale));
    }

    /** @return the key of the one-line notice a thrower gets for {@code refusal} */
    static String refusalKey(Refusal refusal) {
        return switch (refusal) {
            case PERMISSION -> REFUSED_PERMISSION;
            case OPT_OUT, IMMUNE -> REFUSED_TARGET;
            case STATE -> REFUSED_STATE;
            case COOLDOWN -> REFUSED_COOLDOWN;
        };
    }
}
