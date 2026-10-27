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
import java.util.Optional;
import java.util.Random;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioSelectionTest {

    private static final long SEED = 7L;
    private static final int DRAWS = 500;

    private static RadioSong disc(Material material) {
        return VanillaDiscs.ofMaterial(material).orElseThrow();
    }

    private static RadioSelection selectionOf(List<RadioSong> catalog) {
        return new RadioSelection(catalog, new Random(SEED));
    }

    private static RadioSelection fullCatalog() {
        return selectionOf(new ArrayList<RadioSong>(VanillaDiscs.ALL));
    }

    @DisplayName("Once starts no successor after a title ends")
    @Test
    void onceStartsNoSuccessor() {
        RadioSelection selection = fullCatalog();

        Assertions.assertEquals(Optional.empty(), selection.afterEnd(disc(Material.MUSIC_DISC_13), PlayMode.ONCE), "once must not start anything after the end");
    }

    @DisplayName("Loop starts the same title again after it ends")
    @Test
    void loopRestartsTheSameTitle() {
        RadioSong cat = disc(Material.MUSIC_DISC_CAT);

        Assertions.assertEquals(Optional.of(cat), fullCatalog().afterEnd(cat, PlayMode.LOOP), "loop must restart the title that ended");
    }

    @DisplayName("Shuffle never starts the title that just ended")
    @Test
    void shuffleNeverRepeatsTheTitleThatEnded() {
        RadioSelection selection = fullCatalog();
        for (RadioSong ended : VanillaDiscs.ALL) {
            for (int draw = 0; draw < DRAWS; draw++) {
                RadioSong successor = selection.afterEnd(ended, PlayMode.SHUFFLE).orElseThrow();

                Assertions.assertNotEquals(ended, successor, "shuffle must not repeat " + ended.title() + " right after it ended");
            }
        }
    }

    @DisplayName("Shuffle with one title repeats it, as there is no other")
    @Test
    void shuffleWithOneTitleRepeatsIt() {
        RadioSong only = disc(Material.MUSIC_DISC_13);

        Assertions.assertEquals(Optional.of(only), selectionOf(List.of(only)).afterEnd(only, PlayMode.SHUFFLE), "with one title shuffle must repeat it");
    }

    @DisplayName("An empty catalog yields no title, neither after an end nor on next")
    @Test
    void emptyCatalogYieldsNothing() {
        RadioSelection selection = selectionOf(List.of());

        Assertions.assertEquals(Optional.empty(), selection.afterEnd(disc(Material.MUSIC_DISC_13), PlayMode.SHUFFLE), "an empty catalog must not start a successor");
        Assertions.assertEquals(Optional.empty(), selection.next(null, null, PlayMode.ONCE), "an empty catalog must not yield a next title");
    }

    @DisplayName("Next in once and loop goes through the catalog in order and wraps at the end")
    @Test
    void nextFollowsTheCatalogInOrder() {
        RadioSong first = disc(Material.MUSIC_DISC_13);
        RadioSong second = disc(Material.MUSIC_DISC_CAT);
        RadioSong last = VanillaDiscs.ALL.getLast();
        RadioSelection selection = fullCatalog();

        Assertions.assertEquals(Optional.of(second), selection.next(first, null, PlayMode.ONCE), "once must go to the next title in order");
        Assertions.assertEquals(Optional.of(second), selection.next(first, null, PlayMode.LOOP), "loop must go to the next title in order too");
        Assertions.assertEquals(Optional.of(first), selection.next(last, null, PlayMode.ONCE), "the last title must wrap to the first");
    }

    @DisplayName("Next without a current title starts at the first title in order")
    @Test
    void nextWithoutCurrentStartsAtTheFirst() {
        Assertions.assertEquals(Optional.of(VanillaDiscs.ALL.getFirst()), fullCatalog().next(null, RadioCategories.MINECRAFT, PlayMode.ONCE), "next without a current title must start the first title of the last opened category");
    }

    @DisplayName("Next in shuffle picks another title than the current one")
    @Test
    void nextInShuffleIsAnotherTitle() {
        RadioSelection selection = fullCatalog();
        for (RadioSong current : VanillaDiscs.ALL) {
            for (int draw = 0; draw < DRAWS; draw++) {
                RadioSong picked = selection.next(current, null, PlayMode.SHUFFLE).orElseThrow();

                Assertions.assertNotEquals(current, picked, "shuffle must not pick " + current.title() + " again after it");
            }
        }
    }

    @DisplayName("Shuffle after a vanilla disc stays in the Minecraft category")
    @Test
    void shuffleStaysInTheCategoryOfTheEndedTitle() {
        RadioSelection selection = selectionOf(mixedCatalog());
        RadioSong ended = disc(Material.MUSIC_DISC_13);
        for (int draw = 0; draw < DRAWS; draw++) {
            RadioSong successor = selection.afterEnd(ended, PlayMode.SHUFFLE).orElseThrow();

            Assertions.assertEquals(RadioCategories.MINECRAFT, successor.category(), "shuffle must not leave the Minecraft category, got " + successor.title());
        }
    }

    @DisplayName("Shuffle after a MIDI song of a declared category stays in that category")
    @Test
    void shuffleStaysInADeclaredCategory() {
        RadioSelection selection = selectionOf(mixedCatalog());
        RadioSong ended = GAME_ONE;
        for (int draw = 0; draw < DRAWS; draw++) {
            RadioSong successor = selection.afterEnd(ended, PlayMode.SHUFFLE).orElseThrow();

            Assertions.assertEquals(GAMES, successor.category(), "shuffle must stay in games, got " + successor.title());
            Assertions.assertNotEquals(ended, successor, "shuffle must not repeat the title that ended");
        }
    }

    @DisplayName("Next in a category wraps inside the category, not over the whole catalog")
    @Test
    void nextWrapsInsideTheCategory() {
        RadioSelection selection = selectionOf(mixedCatalog());

        Assertions.assertEquals(Optional.of(GAME_ONE), selection.next(GAME_TWO, null, PlayMode.ONCE), "the last game must wrap to the first game");
    }

    @DisplayName("Next without a current title starts the first title of the last opened category")
    @Test
    void nextWithoutCurrentUsesTheLastOpenedCategory() {
        RadioSelection selection = selectionOf(mixedCatalog());

        Assertions.assertEquals(Optional.of(GAME_ONE), selection.next(null, GAMES, PlayMode.ONCE), "next must start the first game when games was opened last");
    }

    @DisplayName("Next without a current title and without an opened category is empty")
    @Test
    void nextWithoutCurrentAndWithoutCategoryIsEmpty() {
        RadioSelection selection = selectionOf(mixedCatalog());

        Assertions.assertEquals(Optional.empty(), selection.next(null, null, PlayMode.ONCE), "next without any category must yield nothing");
    }

    @DisplayName("A category with one title repeats it in shuffle")
    @Test
    void singleTitleCategoryRepeatsInShuffle() {
        RadioSong lone = midi("lone", GAMES);
        RadioSelection selection = selectionOf(List.of(disc(Material.MUSIC_DISC_13), lone));

        Assertions.assertEquals(Optional.of(lone), selection.afterEnd(lone, PlayMode.SHUFFLE), "a category with one title must repeat it");
    }

    private static final RadioCategory GAMES = new RadioCategory("games", "Spiele", "Games", Material.EMERALD, 10);
    private static final RadioSong GAME_ONE = midi("one", GAMES);
    private static final RadioSong GAME_TWO = midi("two", GAMES);

    private static RadioSong midi(String id, RadioCategory category) {
        return new MidiSong(id, id, "Beispiel", Material.MUSIC_DISC_WARD, category, List.of(new MidiNote(0, RadioInstrument.HARP, 12, 100)));
    }

    private static List<RadioSong> mixedCatalog() {
        List<RadioSong> catalog = new ArrayList<>(VanillaDiscs.ALL);
        catalog.add(GAME_ONE);
        catalog.add(GAME_TWO);
        return catalog;
    }
}
