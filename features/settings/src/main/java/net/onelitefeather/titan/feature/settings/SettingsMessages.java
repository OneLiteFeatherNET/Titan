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
package net.onelitefeather.titan.feature.settings;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.SettingKey;

/**
 * The texts of the settings menu in German and English. Option names and descriptions are
 * translated per setting; a setting without a translation shows its translation key.
 */
final class SettingsMessages implements AutoCloseable {

    static final String TITLE = "titan.settings.title";
    static final String ITEM = "titan.settings.item";
    static final String ITEM_HINT = "titan.settings.item.hint";
    static final String STATUS_ON = "titan.settings.status.on";
    static final String STATUS_OFF = "titan.settings.status.off";
    static final String HINT = "titan.settings.hint";

    /**
     * Every key the column renders with a fixed text; the bundle test checks the property files
     * against it.
     */
    static final List<String> KEYS = List.of(TITLE, ITEM, ITEM_HINT, STATUS_ON, STATUS_OFF, HINT);

    private static final String BUNDLE = "titan.settings.messages";
    private static final String OPTION_PREFIX = "titan.settings.option.";

    private final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("titan", "settings"));
    private boolean registered;

    SettingsMessages() {
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

    /** The language a player is shown the menu in; any language without texts reads English. */
    static Locale languageOf(Locale locale) {
        return Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH;
    }

    Component title(Locale language) {
        return text(TITLE, language);
    }

    /** The name of the hotbar item, in the brand gradient and not italic. */
    Component itemName(Locale language) {
        return italicOff(text(ITEM, language));
    }

    /** The one lore line of the hotbar item: how to open the menu. */
    Component itemHint(Locale language) {
        return italicOff(text(ITEM_HINT, language));
    }

    /** @return the option's name in the brand gradient, rendered for {@code language} */
    static Component name(BooleanSetting setting, Locale language) {
        return italicOff(text(optionKey(setting.key(), "name"), language));
    }

    /** @return the option's description, which the menu shows under its icon */
    static Component description(BooleanSetting setting, Locale language) {
        return italicOff(text(optionKey(setting.key(), "description"), language));
    }

    /**
     * @return the state of an option as the text of its switch: a green check when on, a red cross
     *         when off
     */
    static Component toggleName(boolean enabled, Locale language) {
        return italicOff(text(enabled ? STATUS_ON : STATUS_OFF, language));
    }

    /** @return how to switch an option, the lore of its switch */
    static Component toggleHint(Locale language) {
        return italicOff(text(HINT, language));
    }

    /**
     * Translation key of an option's text, e.g.
     * {@code titan.settings.option.titan.tag.enabled.name}.
     */
    static String optionKey(SettingKey key, String part) {
        return OPTION_PREFIX + key.namespace() + "." + key.value() + "." + part;
    }

    private static Component text(String key, Locale language) {
        return GlobalTranslator.render(Component.translatable(key), language);
    }

    private static Component italicOff(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
