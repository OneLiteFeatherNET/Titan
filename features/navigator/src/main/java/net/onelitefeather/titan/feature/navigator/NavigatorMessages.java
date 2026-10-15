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
import java.util.ArrayList;
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
    private static final String LEFT_CONNECT = "titan.navigator.click.left_connect";
    private static final String LEFT_PICK = "titan.navigator.click.left_pick";
    private static final String RIGHT_PORTAL = "titan.navigator.click.right_portal";

    /** Every key this class renders; the bundle test checks them against the property files. */
    static final List<String> KEYS = List.of(LEFT_CONNECT, LEFT_PICK, RIGHT_PORTAL);

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
     * The click hint lines of one entry, one per click: the left click first, then the right click
     * only with a portal. The left click is "choose a server" only while the picker opens for the
     * mode.
     */
    List<Component> clickHints(Locale locale, boolean serverPicker, boolean portal) {
        List<Component> hints = new ArrayList<>();
        hints.add(render(serverPicker ? LEFT_PICK : LEFT_CONNECT, locale));
        if (portal) {
            hints.add(render(RIGHT_PORTAL, locale));
        }
        return hints;
    }

    private static Component render(String key, Locale locale) {
        return GlobalTranslator.render(Component.translatable(key), locale);
    }
}
