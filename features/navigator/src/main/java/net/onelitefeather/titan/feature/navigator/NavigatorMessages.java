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
package net.onelitefeather.titan.feature.navigator;

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
 * Translated texts of the navigator. Rendering is explicit via {@link GlobalTranslator}: a
 * translatable component without the render flag stays empty on Minestom 26.1.
 */
@Singleton
final class NavigatorMessages implements AutoCloseable {

    private static final String BUNDLE = "titan.navigator.messages";
    private static final String CLICK_CONNECT = "titan.navigator.click.connect";
    private static final String CLICK_CONNECT_PORTAL = "titan.navigator.click.connect_portal";
    private static final String CLICK_PICK = "titan.navigator.click.pick";
    private static final String CLICK_PICK_PORTAL = "titan.navigator.click.pick_portal";

    /** Every key this class renders; the bundle test checks them against the property files. */
    static final List<String> KEYS = List.of(CLICK_CONNECT, CLICK_CONNECT_PORTAL, CLICK_PICK, CLICK_PICK_PORTAL);

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "navigator"));
    private boolean registered;

    NavigatorMessages() {
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
     * The click hint line of one entry. The left click is "choose a server" only while the picker
     * opens for the mode, and "connect" otherwise; the right click is named only with a portal.
     */
    Component clickHint(Locale locale, boolean serverPicker, boolean portal) {
        String key = serverPicker ? (portal ? CLICK_PICK_PORTAL : CLICK_PICK) : (portal ? CLICK_CONNECT_PORTAL : CLICK_CONNECT);
        return GlobalTranslator.render(Component.translatable(key), locale);
    }
}
