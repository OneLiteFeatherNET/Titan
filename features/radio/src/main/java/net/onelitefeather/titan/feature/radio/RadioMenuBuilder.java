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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.theevilreaper.aves.i18n.TextData;
import net.theevilreaper.aves.inventory.CustomInventory;
import net.theevilreaper.aves.inventory.GlobalTranslatedInventoryBuilder;
import net.theevilreaper.aves.inventory.holder.InventoryHolderImpl;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.item.TranslatedItem;

/**
 * The six radio inventories: two menus, each in three play modes. Every inventory is one Aves
 * builder, and Aves renders its texts for the viewer's locale, so the inventories are shared by
 * everyone who opens them. A click on a builder's inventory is handled by {@link RadioModule}:
 * Aves' own click handling does not run for translated builders.
 */
final class RadioMenuBuilder {

    private static final ItemStack FILLER = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();
    private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN);

    /** One of the six inventories: which menu, in which play mode. */
    record Variant(RadioMenu menu, PlayMode mode) {

        Variant {
            Objects.requireNonNull(menu, "menu must not be null");
            Objects.requireNonNull(mode, "mode must not be null");
        }
    }

    private final List<MidiSong> songs;
    private final Map<Variant, GlobalTranslatedInventoryBuilder> builders = new HashMap<>();

    RadioMenuBuilder(List<MidiSong> songs) {
        this.songs = List.copyOf(Objects.requireNonNull(songs, "songs must not be null"));
        for (RadioMenu menu : RadioMenu.values()) {
            for (PlayMode mode : PlayMode.values()) {
                this.builders.put(new Variant(menu, mode), builder(menu, mode));
            }
        }
    }

    /**
     * Registers every builder and builds its inventory for each supported language up front. Aves
     * fills an inventory's contents on the next tick, so a viewer who opens one later sees it full.
     */
    void register() {
        for (GlobalTranslatedInventoryBuilder builder : this.builders.values()) {
            builder.register();
            for (Locale language : LANGUAGES) {
                builder.getInventory(language);
            }
        }
    }

    void unregister() {
        this.builders.values().forEach(GlobalTranslatedInventoryBuilder::unregister);
    }

    /** The language a viewer reads: German for German clients, English for every other client. */
    static Locale languageOf(Locale locale) {
        return locale != null && Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH;
    }

    /** The inventory of {@code menu} in {@code mode}, as {@code locale} sees it. */
    Inventory inventory(RadioMenu menu, PlayMode mode, Locale locale) {
        return this.builders.get(new Variant(menu, mode)).getInventory(languageOf(locale));
    }

    /** Which of the six inventories {@code inventory} is, or nothing when it is none of ours. */
    Optional<Variant> ownerOf(AbstractInventory inventory) {
        if (inventory instanceof CustomInventory custom && custom.getHolder() instanceof InventoryHolderImpl holder) {
            Object builder = holder.inventoryBuilder();
            return this.builders.entrySet().stream().filter(entry -> entry.getValue() == builder).map(Map.Entry::getKey).findFirst();
        }
        return Optional.empty();
    }

    /** Whether the switch to the songs menu is shown: only when there are songs. */
    boolean switchShown() {
        return !this.songs.isEmpty();
    }

    private GlobalTranslatedInventoryBuilder builder(RadioMenu menu, PlayMode mode) {
        GlobalTranslatedInventoryBuilder builder = new EnglishWithoutLocale(RadioMenuLayout.TYPE);
        builder.setTitleData(new TextData(menu == RadioMenu.VANILLA ? RadioMessages.MENU_VANILLA : RadioMessages.MENU_SONGS));
        builder.setLayout(InventoryLayout.fromType(RadioMenuLayout.TYPE));
        builder.setDataLayoutFunction(previous -> layout(menu, mode));
        return builder;
    }

    private InventoryLayout layout(RadioMenu menu, PlayMode mode) {
        InventoryLayout layout = InventoryLayout.fromType(RadioMenuLayout.TYPE);
        for (int slot = 0; slot < RadioMenuLayout.SIZE; slot++) {
            layout.setItem(slot, FILLER);
        }
        List<? extends RadioSong> discs = menu == RadioMenu.VANILLA ? VanillaDiscs.ALL : this.songs;
        int shown = Math.min(discs.size(), RadioMenuLayout.discSlots(menu));
        for (int slot = 0; slot < shown; slot++) {
            layout.setItem(slot, disc(discs.get(slot)).toNonClickSlot());
        }
        layout.setItem(RadioMenuLayout.STOP_SLOT, TranslatedItem.of(Material.BARRIER).setDisplayName(new TextData(RadioMessages.STOP)).toNonClickSlot());
        layout.setItem(RadioMenuLayout.NEXT_SLOT, TranslatedItem.of(Material.ARROW).setDisplayName(new TextData(RadioMessages.NEXT)).toNonClickSlot());
        layout.setItem(RadioMenuLayout.MODE_SLOT, TranslatedItem.of(Material.REPEATER).setDisplayName(new TextData(modeName(mode))).setLore(new TextData(RadioMessages.MODE_HINT)).toNonClickSlot());
        if (menu == RadioMenu.VANILLA && switchShown()) {
            layout.setItem(RadioMenuLayout.SWITCH_SLOT, TranslatedItem.of(Material.NOTE_BLOCK).setDisplayName(new TextData(RadioMessages.SWITCH_TO_SONGS)).toNonClickSlot());
        } else if (menu == RadioMenu.SONGS) {
            layout.setItem(RadioMenuLayout.SWITCH_SLOT, TranslatedItem.of(Material.JUKEBOX).setDisplayName(new TextData(RadioMessages.SWITCH_TO_VANILLA)).toNonClickSlot());
        }
        return layout;
    }

    private static TranslatedItem disc(RadioSong song) {
        String lore = song.kind() == RadioKind.VANILLA ? RadioMessages.DISC_LORE_VANILLA : RadioMessages.DISC_LORE_MIDI;
        return TranslatedItem.of(song.disc()).setDisplayName(TextData.of(RadioMessages.DISC_NAME, song.title())).setLore(TextData.of(lore, song.artist()));
    }

    private static String modeName(PlayMode mode) {
        return switch (mode) {
            case ONCE -> RadioMessages.MODE_ONCE;
            case LOOP -> RadioMessages.MODE_LOOP;
            case SHUFFLE -> RadioMessages.MODE_SHUFFLE;
        };
    }

    /**
     * Aves asks for the inventory without a locale when it registers a builder; that inventory
     * reads English, as in the lobby switcher.
     */
    private static final class EnglishWithoutLocale extends GlobalTranslatedInventoryBuilder {

        EnglishWithoutLocale(InventoryType type) {
            super(type);
        }

        @Override
        public Inventory getInventory(Locale locale) {
            return super.getInventory(locale == null ? Locale.ENGLISH : locale);
        }
    }
}
