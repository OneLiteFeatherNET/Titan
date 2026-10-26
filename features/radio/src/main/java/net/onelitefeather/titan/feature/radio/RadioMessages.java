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
package net.onelitefeather.titan.feature.radio;

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
 * Translated texts of the radio, in German and English. Each text is a translation key rendered by
 * the {@link GlobalTranslator}; a translatable component without the render step stays empty on
 * Minestom 26.1.
 */
@Singleton
final class RadioMessages implements AutoCloseable {

    static final String ITEM_NAME = "titan.radio.item.name";
    static final String ITEM_HINT = "titan.radio.item.hint";
    static final String MENU_VANILLA = "titan.radio.menu.vanilla";
    static final String MENU_SONGS = "titan.radio.menu.songs";
    static final String SWITCH_TO_SONGS = "titan.radio.switch.songs";
    static final String SWITCH_TO_VANILLA = "titan.radio.switch.vanilla";
    static final String DISC_NAME = "titan.radio.disc.name";
    static final String DISC_LORE_VANILLA = "titan.radio.disc.lore.vanilla";
    static final String DISC_LORE_MIDI = "titan.radio.disc.lore.midi";
    static final String STOP = "titan.radio.stop";
    static final String NEXT = "titan.radio.next";
    static final String MODE_ONCE = "titan.radio.mode.once";
    static final String MODE_LOOP = "titan.radio.mode.loop";
    static final String MODE_SHUFFLE = "titan.radio.mode.shuffle";
    static final String MODE_HINT = "titan.radio.mode.hint";
    static final String PLAYING = "titan.radio.playing";
    static final String STOPPED = "titan.radio.stopped";

    /** Every key the radio renders; the bundle test checks the property files against it. */
    static final List<String> KEYS = List.of(ITEM_NAME, ITEM_HINT, MENU_VANILLA, MENU_SONGS, SWITCH_TO_SONGS, SWITCH_TO_VANILLA, DISC_NAME, DISC_LORE_VANILLA, DISC_LORE_MIDI, STOP, NEXT, MODE_ONCE, MODE_LOOP, MODE_SHUFFLE, MODE_HINT, PLAYING, STOPPED);

    private static final String BUNDLE = "titan.radio.messages";

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "radio"));
    private boolean registered;

    RadioMessages() {
        this.store.defaultLocale(Locale.ENGLISH);
        this.store.registerAll(Locale.ENGLISH, ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH), false);
        this.store.registerAll(Locale.GERMAN, ResourceBundle.getBundle(BUNDLE, Locale.GERMAN), false);
    }

    /** Adds the store to the {@link GlobalTranslator}; idempotent. */
    @PostConstruct
    void register() {
        if (!this.registered) {
            this.registered = GlobalTranslator.translator().addSource(this.store);
        }
    }

    /** Removes the store again; idempotent. */
    @PreDestroy
    @Override
    public void close() {
        if (this.registered) {
            GlobalTranslator.translator().removeSource(this.store);
            this.registered = false;
        }
    }

    /**
     * The text under {@code key}, rendered for {@code locale}; {@code arguments} fill its
     * {@code <arg:n>} tags.
     */
    static Component text(String key, Locale locale, String... arguments) {
        Component[] parts = new Component[arguments.length];
        for (int index = 0; index < arguments.length; index++) {
            parts[index] = Component.text(arguments[index]);
        }
        return GlobalTranslator.render(Component.translatable(key).arguments(parts), locale);
    }

    /** The name of the hotbar item, in the player's language. */
    Component itemName(Locale locale) {
        return text(ITEM_NAME, locale);
    }

    /** The one grey line of the hotbar item, in the player's language. */
    Component itemHint(Locale locale) {
        return text(ITEM_HINT, locale);
    }
}
