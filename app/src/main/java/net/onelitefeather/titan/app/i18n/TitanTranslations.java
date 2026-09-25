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
package net.onelitefeather.titan.app.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * The lobby's {@link MiniMessageTranslationStore}: the first {@code TranslationStore} in Titan
 * (see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 5).
 *
 * <p><b>Bundles.</b> {@code app/src/main/resources/lang/titan_en.properties} (the fallback/default
 * locale, English) and {@code titan_de.properties} are read as UTF-8 explicitly ({@link
 * #bundle(Locale)}) rather than through the platform default charset {@link
 * ResourceBundle#getBundle(String, Locale)} would otherwise use for {@code .properties} files, so
 * the German umlauts in {@code titan_de.properties} survive the round trip.
 *
 * <p><b>Placeholder syntax.</b> Bundle values are MiniMessage strings that reference a
 * translatable component's arguments positionally, with the built-in {@code <arg:N>} tag
 * (0-indexed, {@code N} matching the position in {@link
 * net.kyori.adventure.text.TranslatableComponent#arguments()}) - for example
 * {@code Component.translatable(TitanTranslations.CONFIG_RELOAD_APPLIED, Component.text(restartedModules))}
 * fills {@code <arg:0>} in {@code titan.config.reload.applied}. This is the store's own built-in
 * mechanism ({@code net.kyori.adventure.text.minimessage.translation.ArgumentTag}); no
 * named-argument
 * variant is used, so every key constant below documents its arguments by position.
 *
 * <p><b>Wiring.</b> {@link #register(GlobalTranslator)} takes the translator to register the store
 * with as a parameter instead of reaching for the process-wide
 * {@link GlobalTranslator#translator()}
 * singleton itself - production code (a later wave) calls
 * {@code TitanTranslations.register(GlobalTranslator.translator())} once during bootstrap. Tests
 * must not mutate that shared singleton (F.I.R.S.T. - Independent): they build their own store with
 * {@link #createStore()} and render through the store/{@code Translator} instance directly, with
 * {@link net.kyori.adventure.translation.Translator#translate(net.kyori.adventure.text.TranslatableComponent,
 * java.util.Locale)}, never through
 * {@link GlobalTranslator#render(net.kyori.adventure.text.Component,
 * Locale)}.
 */
public final class TitanTranslations {

    /** No configuration changes were applied. Takes no arguments. */
    public static final String CONFIG_RELOAD_UNCHANGED = "titan.config.reload.unchanged";

    /** Configuration changes were applied. Argument 0: the restarted modules. */
    public static final String CONFIG_RELOAD_APPLIED = "titan.config.reload.applied";

    /** A configuration change was rejected. Argument 0: the key(s). Argument 1: the reason. */
    public static final String CONFIG_RELOAD_REJECTED = "titan.config.reload.rejected";

    /** Reloading the configuration failed. Argument 0: the file. Argument 1: the location. */
    public static final String CONFIG_RELOAD_FAILED = "titan.config.reload.failed";

    private static final Key STORE_KEY = Key.key("titan", "lobby");
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final Locale[] BUNDLE_LOCALES = {Locale.ENGLISH, Locale.GERMAN};

    private TitanTranslations() {
    }

    /**
     * Builds a fresh {@link MiniMessageTranslationStore}, with the English bundle registered as
     * {@link #DEFAULT_LOCALE its default/fallback locale} and the German bundle alongside it. Every
     * call returns an independent store; nothing here is shared, global, mutable state.
     */
    public static MiniMessageTranslationStore createStore() {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(STORE_KEY);
        store.defaultLocale(DEFAULT_LOCALE);
        for (Locale locale : BUNDLE_LOCALES) {
            store.registerAll(locale, bundle(locale), false);
        }
        return store;
    }

    /**
     * Builds a store with {@link #createStore()} and adds it to the given {@code translator} as a
     * source. Production code passes {@link GlobalTranslator#translator()}; tests pass a translator
     * of their own so the real, process-wide singleton is never touched.
     *
     * @param translator the translator to register the store with
     * @return the store that was added, so the caller can keep a reference to it if needed
     */
    public static MiniMessageTranslationStore register(GlobalTranslator translator) {
        MiniMessageTranslationStore store = createStore();
        translator.addSource(store);
        return store;
    }

    private static ResourceBundle bundle(Locale locale) {
        String resource = "/lang/titan_" + locale.getLanguage() + ".properties";
        try (InputStream input = TitanTranslations.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing translation bundle on the classpath: " + resource);
            }
            return new PropertyResourceBundle(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read translation bundle: " + resource, e);
        }
    }
}
