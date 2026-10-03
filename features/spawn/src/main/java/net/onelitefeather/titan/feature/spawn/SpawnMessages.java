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
package net.onelitefeather.titan.feature.spawn;

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
 * Translated texts of the spawn column. Rendering is explicit via {@link GlobalTranslator}: a
 * translatable component without the render flag stays empty on Minestom 26.1.
 */
@Singleton
final class SpawnMessages implements AutoCloseable {

    private static final String BUNDLE = "titan.spawn.messages";
    private static final String RETURN_DONE = "titan.spawn.return.done";
    private static final String RETURN_NO_SPAWN = "titan.spawn.return.no_spawn";

    /** Every key this class renders; the bundle test checks them against the property files. */
    static final List<String> KEYS = List.of(RETURN_DONE, RETURN_NO_SPAWN);

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "spawn"));
    private boolean registered;

    SpawnMessages() {
        store.defaultLocale(Locale.ENGLISH);
        store.registerAll(Locale.ENGLISH, ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH), false);
        store.registerAll(Locale.GERMAN, ResourceBundle.getBundle(BUNDLE, Locale.GERMAN), false);
    }

    /** Adds the store to the {@link GlobalTranslator}; idempotent. */
    @PostConstruct
    void register() {
        if (!registered) {
            registered = GlobalTranslator.translator().addSource(store);
        }
    }

    /** Removes the store again; idempotent. */
    @PreDestroy
    @Override
    public void close() {
        if (registered) {
            GlobalTranslator.translator().removeSource(store);
            registered = false;
        }
    }

    Component returned(Locale locale) {
        return render(Component.translatable(RETURN_DONE), locale);
    }

    Component noSpawn(Locale locale) {
        return render(Component.translatable(RETURN_NO_SPAWN), locale);
    }

    private static Component render(Component translatable, Locale locale) {
        return GlobalTranslator.render(translatable, locale);
    }
}
