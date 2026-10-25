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
import java.util.OptionalInt;
import java.util.ResourceBundle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * Translated texts of the navigator. Each text is a translation key in the bundles, rendered by
 * {@link GlobalTranslator}: a translatable component without the render flag stays empty on
 * Minestom 26.1.
 */
@Singleton
final class NavigatorMessages implements AutoCloseable {

    static final String TITLE = "titan.navigator.title";
    static final String PICKER_TITLE = "titan.navigator.picker.title";
    static final String PLAYERS = "titan.navigator.players";
    static final String KEY_LEFT = "titan.navigator.key.left";
    static final String KEY_RIGHT = "titan.navigator.key.right";
    static final String KEY_SHIFT = "titan.navigator.key.shift";
    static final String ACTION_QUICK_JOIN = "titan.navigator.action.quick_join";
    static final String ACTION_CHOOSE_SERVER = "titan.navigator.action.choose_server";
    static final String ACTION_CONNECT = "titan.navigator.action.connect";
    static final String ACTION_PORTAL = "titan.navigator.action.portal";
    static final String PICKER_FULL = "titan.navigator.picker.full";
    static final String PICKER_NOT_READY = "titan.navigator.picker.not_ready";
    static final String PICKER_JOIN = "titan.navigator.picker.join";
    static final String REFUSAL_FULL = "titan.navigator.picker.refusal.full";
    static final String REFUSAL_NOT_READY = "titan.navigator.picker.refusal.not_ready";
    static final String REFUSAL_UNAVAILABLE = "titan.navigator.picker.refusal.unavailable";
    static final String PICKER_EMPTY = "titan.navigator.picker.empty";
    static final String SORT_ITEM = "titan.navigator.picker.sort.item";
    static final String SORT_NAME = "titan.navigator.picker.sort.name";
    static final String SORT_MOST_PLAYERS = "titan.navigator.picker.sort.most_players";
    static final String SORT_MOST_FREE = "titan.navigator.picker.sort.most_free";
    static final String SORT_STATUS = "titan.navigator.picker.sort.status";
    static final String SORT_HINT = "titan.navigator.picker.sort.hint";
    static final String PAGE_PREVIOUS = "titan.navigator.picker.page.previous";
    static final String PAGE_NEXT = "titan.navigator.picker.page.next";
    static final String PAGE_INDICATOR = "titan.navigator.picker.page.indicator";
    static final String PICKER_BACK = "titan.navigator.picker.back";
    static final String STAGING_HINT = "titan.navigator.hint.staging";

    /** Every key the navigator renders; the bundle test checks the property files against it. */
    static final List<String> KEYS = List.of(TITLE, PICKER_TITLE, PLAYERS, KEY_LEFT, KEY_RIGHT, KEY_SHIFT, ACTION_QUICK_JOIN, ACTION_CHOOSE_SERVER, ACTION_CONNECT, ACTION_PORTAL, PICKER_FULL, PICKER_NOT_READY, PICKER_JOIN, REFUSAL_FULL, REFUSAL_NOT_READY, REFUSAL_UNAVAILABLE, PICKER_EMPTY, SORT_ITEM, SORT_NAME, SORT_MOST_PLAYERS, SORT_MOST_FREE, SORT_STATUS, SORT_HINT, PAGE_PREVIOUS, PAGE_NEXT, PAGE_INDICATOR, PICKER_BACK, STAGING_HINT);

    private static final String BUNDLE = "titan.navigator.messages";
    private static final String SEPARATOR = "━━━━━━━━━━━━";
    private static final String LABEL_COLOR = "<#8c96a8>";
    /** The marker in front of every click and state line. */
    static final String ARROW = "<#00ddff>▸ </#00ddff>";

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
     * One translated text, rendered for {@code locale}; {@code arguments} fill its {@code <arg:n>}
     * tags.
     */
    static Component text(String key, Locale locale, Component... arguments) {
        return GlobalTranslator.render(Component.translatable(key).arguments(arguments), locale);
    }

    /**
     * The lore of one mode entry: a separator in the mode's gradient, the players line when the
     * count
     * is known, a blank line, then one line per click action.
     */
    static List<Component> modeLore(String gradient, OptionalInt online, OptionalInt max, List<ClickAction> actions, Locale locale) {
        MiniMessage mini = MiniMessage.miniMessage();
        List<Component> lore = new ArrayList<>();
        lore.add(mini.deserialize("<!i>" + gradient + SEPARATOR + "</gradient>"));
        if (online.isPresent()) {
            lore.add(playersLine(online.getAsInt(), max, locale));
        }
        lore.add(Component.empty());
        for (ClickAction action : actions) {
            lore.add(clickLine(action, locale));
        }
        return List.copyOf(lore);
    }

    /** The players of an entry, with the maximum after a slash when it is known. */
    static Component playersLine(int online, OptionalInt max, Locale locale) {
        MiniMessage mini = MiniMessage.miniMessage();
        TagResolver label = Placeholder.component("label", text(PLAYERS, locale));
        TagResolver count = Placeholder.unparsed("online", String.valueOf(online));
        if (max.isEmpty()) {
            return mini.deserialize("<!i>" + LABEL_COLOR + "<label>  </#8c96a8><white><online></white>", label, count);
        }
        TagResolver limit = Placeholder.unparsed("max", String.valueOf(max.getAsInt()));
        return mini.deserialize("<!i>" + LABEL_COLOR + "<label>  </#8c96a8><white><online></white>" + LABEL_COLOR + " / <max></#8c96a8>", label, count, limit);
    }

    /** One grey line of its own that explains an entry, such as a team-only test environment. */
    static Component hintLine(String key, Locale locale) {
        return MiniMessage.miniMessage().deserialize("<!i><gray><hint></gray>", Placeholder.component("hint", text(key, locale)));
    }

    /** The title of the navigator, styled as its gradient, in {@code locale}. */
    static Component title(Locale locale) {
        return text(TITLE, locale);
    }

    private static Component clickLine(ClickAction action, Locale locale) {
        TagResolver key = Placeholder.component("key", text(action.keyKey(), locale));
        TagResolver label = Placeholder.component("action", text(action.labelKey(), locale));
        return MiniMessage.miniMessage().deserialize("<!i>" + ARROW + "<white><key>  </white>" + LABEL_COLOR + "<action></#8c96a8>", key, label);
    }
}
