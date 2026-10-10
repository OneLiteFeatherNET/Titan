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
package net.onelitefeather.titan.feature.lobbyswitcher;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.avaje.inject.Profile;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * Translated texts of the lobby switcher. Rendering is explicit via {@link GlobalTranslator}: a
 * translatable component without the render flag stays empty on Minestom 26.1.
 */
@Singleton
@Profile(LobbySwitcherModule.CLOUDNET)
final class LobbySwitcherMessages implements AutoCloseable {

    private static final String BUNDLE = "titan.lobbyswitcher.messages";

    static final String TITLE = "titan.lobbyswitcher.title";
    static final String ITEM_NAME = "titan.lobbyswitcher.item.name";
    static final String ENTRY_NAME = "titan.lobbyswitcher.entry.name";
    static final String ENTRY_COUNT = "titan.lobbyswitcher.entry.count";
    static final String STATE_CURRENT = "titan.lobbyswitcher.state.current";
    static final String STATE_FULL = "titan.lobbyswitcher.state.full";
    static final String STATE_NOT_READY = "titan.lobbyswitcher.state.not_ready";
    static final String STATE_UNAVAILABLE = "titan.lobbyswitcher.state.unavailable";
    static final String MESSAGE_SENT = "titan.lobbyswitcher.message.sent";
    static final String MESSAGE_CURRENT = "titan.lobbyswitcher.message.current";
    static final String MESSAGE_FULL = "titan.lobbyswitcher.message.full";
    static final String MESSAGE_NOT_READY = "titan.lobbyswitcher.message.not_ready";
    static final String MESSAGE_GONE = "titan.lobbyswitcher.message.gone";
    static final String MESSAGE_UNAVAILABLE = "titan.lobbyswitcher.message.unavailable";

    /** Every key this class renders; the bundle test checks them against the property files. */
    static final List<String> KEYS = List.of(TITLE, ITEM_NAME, ENTRY_NAME, ENTRY_COUNT, STATE_CURRENT, STATE_FULL, STATE_NOT_READY, STATE_UNAVAILABLE, MESSAGE_SENT, MESSAGE_CURRENT, MESSAGE_FULL, MESSAGE_NOT_READY, MESSAGE_GONE, MESSAGE_UNAVAILABLE);

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "lobbyswitcher"));
    private boolean registered;

    LobbySwitcherMessages() {
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

    Component itemName(Locale locale) {
        return render(Component.translatable(ITEM_NAME), locale);
    }

    /** What the player is told about a click; a failed check reads as "unavailable". */
    Component click(Locale locale, SwitcherState decision, String target) {
        Component message = switch (decision) {
            case JOINABLE -> Component.translatable(MESSAGE_SENT, Component.text(target));
            case CURRENT -> Component.translatable(MESSAGE_CURRENT);
            case FULL -> Component.translatable(MESSAGE_FULL);
            case NOT_READY -> Component.translatable(MESSAGE_NOT_READY);
            case GONE -> Component.translatable(MESSAGE_GONE);
            case ERROR -> Component.translatable(MESSAGE_UNAVAILABLE);
        };
        return render(message, locale);
    }

    /** Lobbies cannot be listed, e.g. because this lobby does not know its own task. */
    Component unavailable(Locale locale) {
        return render(Component.translatable(MESSAGE_UNAVAILABLE), locale);
    }

    Component render(Component translatable, Locale locale) {
        return GlobalTranslator.render(translatable, locale);
    }
}
