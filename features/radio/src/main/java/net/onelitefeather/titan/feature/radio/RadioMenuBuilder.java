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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * Builds the content of one radio window from a snapshot of the player's state. Items are plain
 * stacks rendered for the window's language: a translated item would lose its glint. Every control
 * keeps its slot, and an unavailable one shows as glass.
 */
final class RadioMenuBuilder {

    private static final ItemStack FILLER = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    /**
     * What the window shows.
     *
     * @param state       the overview or the open category, and the page
     * @param categories  the overview categories, in order; used in the overview only
     * @param titles      the titles of the open category, in order; empty in the overview
     * @param mode        the player's play mode
     * @param volume      the player's volume in percent
     * @param playing     the title that plays for this player, or {@code null}
     * @param volumeLater whether {@code playing} started at another volume than {@code volume}
     * @param canNext     whether Next has a title to start
     */
    record MenuView(RadioSessionState state, List<RadioCategory> categories, List<RadioSong> titles,
                    PlayMode mode, int volume, RadioSong playing, boolean volumeLater,
                    boolean canNext) {

        MenuView {
            Objects.requireNonNull(state, "state must not be null");
            categories = List.copyOf(categories);
            titles = List.copyOf(titles);
            Objects.requireNonNull(mode, "mode must not be null");
        }
    }

    private RadioMenuBuilder() {
    }

    /** The layout of the window for {@code view}, in the language of {@code locale}. */
    static InventoryLayout build(MenuView view, Locale locale) {
        InventoryLayout layout = InventoryLayout.fromType(RadioWindow.TYPE);
        for (int slot = 0; slot < RadioMenuLayout.SIZE; slot++) {
            layout.setItem(slot, FILLER);
        }
        if (view.state().inOverview()) {
            overview(layout, view, locale);
        } else {
            titles(layout, view, locale);
        }
        controls(layout, view, locale);
        return layout;
    }

    private static void overview(InventoryLayout layout, MenuView view, Locale locale) {
        for (int index = 0; index < view.categories().size() && index < RadioMenuLayout.CONTENT; index++) {
            RadioCategory category = view.categories().get(index);
            layout.setItem(index, item(category.icon(), plain(category.name(locale)), List.of(), false));
        }
    }

    private static void titles(InventoryLayout layout, MenuView view, Locale locale) {
        int first = (PageLayout.clampPage(view.state().page(), view.titles().size()) - 1) * RadioMenuLayout.CONTENT;
        for (int slot = 0; slot < RadioMenuLayout.CONTENT && first + slot < view.titles().size(); slot++) {
            RadioSong song = view.titles().get(first + slot);
            String lore = song.kind() == RadioKind.VANILLA ? RadioMessages.DISC_LORE_VANILLA : RadioMessages.DISC_LORE_MIDI;
            layout.setItem(slot, item(song.disc(), plain(song.title()), List.of(RadioMessages.text(lore, locale, song.artist())), song.equals(view.playing())));
        }
    }

    private static void controls(InventoryLayout layout, MenuView view, Locale locale) {
        boolean inCategory = !view.state().inOverview();
        int pages = PageLayout.pageCount(view.titles().size());
        int page = PageLayout.clampPage(view.state().page(), view.titles().size());
        layout.setItem(RadioMenuLayout.BACK, inCategory ? control(Material.OAK_DOOR, RadioMessages.text(RadioMessages.BACK, locale)) : FILLER);
        layout.setItem(RadioMenuLayout.PREVIOUS, inCategory && page > 1 ? control(Material.SPECTRAL_ARROW, RadioMessages.text(RadioMessages.PAGE_PREVIOUS, locale)) : FILLER);
        layout.setItem(RadioMenuLayout.PAGE_INFO, inCategory ? control(Material.BOOK, RadioMessages.text(RadioMessages.PAGE_INFO, locale, String.valueOf(page), String.valueOf(pages))) : control(Material.BOOK, RadioMessages.text(RadioMessages.CATEGORY_COUNT, locale, String.valueOf(view.categories().size()))));
        layout.setItem(RadioMenuLayout.STOP, control(Material.BARRIER, RadioMessages.text(RadioMessages.STOP, locale)));
        layout.setItem(RadioMenuLayout.NOW_PLAYING, nowPlaying(view, locale));
        layout.setItem(RadioMenuLayout.NEXT, view.canNext() ? control(Material.ARROW, RadioMessages.text(RadioMessages.NEXT, locale)) : FILLER);
        layout.setItem(RadioMenuLayout.MODE, item(Material.REPEATER, RadioMessages.text(modeKey(view.mode()), locale), List.of(RadioMessages.text(RadioMessages.MODE_HINT, locale)), false));
        layout.setItem(RadioMenuLayout.VOLUME, item(Material.NOTE_BLOCK, RadioMessages.text(RadioMessages.NOW_VOLUME, locale, String.valueOf(view.volume())), List.of(RadioMessages.text(RadioMessages.VOLUME_HINT, locale)), false));
        layout.setItem(RadioMenuLayout.FORWARD, inCategory && page < pages ? control(Material.SPECTRAL_ARROW, RadioMessages.text(RadioMessages.PAGE_NEXT, locale)) : FILLER);
    }

    private static ItemStack nowPlaying(MenuView view, Locale locale) {
        List<Component> lore = new ArrayList<>();
        if (view.playing() == null) {
            lore.add(RadioMessages.text(modeKey(view.mode()), locale));
            lore.add(RadioMessages.text(RadioMessages.NOW_VOLUME, locale, String.valueOf(view.volume())));
            return item(Material.JUKEBOX, RadioMessages.text(RadioMessages.NOW_NONE, locale), lore, false);
        }
        RadioSong playing = view.playing();
        lore.add(RadioMessages.text(RadioMessages.NOW_ARTIST, locale, playing.artist()));
        lore.add(RadioMessages.text(RadioMessages.NOW_CATEGORY, locale, playing.category().name(locale)));
        lore.add(RadioMessages.text(modeKey(view.mode()), locale));
        lore.add(RadioMessages.text(RadioMessages.NOW_VOLUME, locale, String.valueOf(view.volume())));
        if (view.volumeLater()) {
            lore.add(RadioMessages.text(RadioMessages.NOW_LATER, locale));
        }
        return item(playing.disc(), RadioMessages.text(RadioMessages.NOW_TITLE, locale, playing.title()), lore, false);
    }

    private static ItemStack control(Material material, Component name) {
        return item(material, name, List.of(), false);
    }

    private static ItemStack item(Material material, Component name, List<Component> lore, boolean glowing) {
        ItemStack.Builder builder = ItemStack.builder(material).customName(name);
        if (!lore.isEmpty()) {
            builder.lore(lore);
        }
        if (glowing) {
            builder.glowing();
        }
        return builder.build();
    }

    private static Component plain(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }

    private static String modeKey(PlayMode mode) {
        return switch (mode) {
            case ONCE -> RadioMessages.MODE_ONCE;
            case LOOP -> RadioMessages.MODE_LOOP;
            case SHUFFLE -> RadioMessages.MODE_SHUFFLE;
        };
    }
}
