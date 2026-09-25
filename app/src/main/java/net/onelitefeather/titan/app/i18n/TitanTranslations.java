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

import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * The lobby's {@link MiniMessageTranslationStore}: the first {@code TranslationStore} in Titan
 * (see {@code openspec/changes/config-reload-feature-flags/design.md}, decision 5).
 *
 * <p><b>Bundles.</b> {@code app/src/main/resources/lang/titan_en.properties} (the fallback/default
 * locale, English) and {@code titan_de.properties} are loaded through the built-in
 * {@link ResourceBundle#getBundle(String, Locale, ClassLoader, ResourceBundle.Control)} ({@link
 * #bundle(Locale)}), base name {@code lang.titan}. Since JDK 9, that reads {@code .properties}
 * files as UTF-8 by default (no longer the platform's default charset), so the German umlauts in
 * {@code titan_de.properties} survive the round trip without any explicit charset handling here.
 * The {@link ResourceBundle.Control} passed in is
 * {@link ResourceBundle.Control#getNoFallbackControl}: without it, {@code ResourceBundle}'s own
 * default control would, on a lookup miss, fall back to the bundle for the JVM's default locale
 * ({@link Locale#getDefault()}) before giving up - which would make {@link #bundle(Locale)}'s
 * result depend on the machine it runs on instead of only on {@code locale}, breaking
 * F.I.R.S.T.'s Repeatable rule. With the no-fallback control, a locale this class was not built to
 * serve raises {@link java.util.MissingResourceException} instead of silently resolving to
 * whatever locale happens to be default.
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

    /**
     * Configuration changes were applied, but none of them - e.g. a {@code features.*} flag, or
     * another non-module key such as {@code titan.config.reload.intervalSeconds} - required
     * restarting a module. Takes no arguments. Added in wave B (see
     * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 5.1): without this key,
     * {@link #CONFIG_RELOAD_APPLIED} would render with an empty module list ("Restarted modules:
     * "),
     * which misreports a reload that only changed flags as one that touched a module.
     */
    public static final String CONFIG_RELOAD_APPLIED_NO_MODULES = "titan.config.reload.applied.nomodules";

    /** A configuration change was rejected. Argument 0: the key(s). Argument 1: the reason. */
    public static final String CONFIG_RELOAD_REJECTED = "titan.config.reload.rejected";

    /** Reloading the configuration failed. Argument 0: the file. Argument 1: the location. */
    public static final String CONFIG_RELOAD_FAILED = "titan.config.reload.failed";

    /**
     * A module could not be restarted even with its previous configuration values and is left
     * disabled - an operator must intervene. Argument 0: the module id. Added in wave B (see
     * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 5.1): the {@code
     * ReloadResult -> Component} mapper reuses {@link #CONFIG_RELOAD_REJECTED}'s wording for
     * nothing, since "disabled" needs its own text - a disabled module is not merely running with
     * its previous values, it is not running at all.
     */
    public static final String CONFIG_RELOAD_DISABLED = "titan.config.reload.disabled";

    private static final Key STORE_KEY = Key.key("titan", "lobby");
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final Locale[] BUNDLE_LOCALES = {Locale.ENGLISH, Locale.GERMAN};
    private static final String BUNDLE_BASE_NAME = "lang.titan";
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

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
        return ResourceBundle.getBundle(
                BUNDLE_BASE_NAME, locale, TitanTranslations.class.getClassLoader(), NO_FALLBACK_CONTROL);
    }
}
