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
        Assertions.assertEquals(Optional.empty(), selection.next(null, PlayMode.ONCE), "an empty catalog must not yield a next title");
    }

    @DisplayName("Next in once and loop goes through the catalog in order and wraps at the end")
    @Test
    void nextFollowsTheCatalogInOrder() {
        RadioSong first = disc(Material.MUSIC_DISC_13);
        RadioSong second = disc(Material.MUSIC_DISC_CAT);
        RadioSong last = VanillaDiscs.ALL.getLast();
        RadioSelection selection = fullCatalog();

        Assertions.assertEquals(Optional.of(second), selection.next(first, PlayMode.ONCE), "once must go to the next title in order");
        Assertions.assertEquals(Optional.of(second), selection.next(first, PlayMode.LOOP), "loop must go to the next title in order too");
        Assertions.assertEquals(Optional.of(first), selection.next(last, PlayMode.ONCE), "the last title must wrap to the first");
    }

    @DisplayName("Next without a current title starts at the first title in order")
    @Test
    void nextWithoutCurrentStartsAtTheFirst() {
        Assertions.assertEquals(Optional.of(VanillaDiscs.ALL.getFirst()), fullCatalog().next(null, PlayMode.ONCE), "next without a current title must start the first title");
    }

    @DisplayName("Next in shuffle picks another title than the current one")
    @Test
    void nextInShuffleIsAnotherTitle() {
        RadioSelection selection = fullCatalog();
        for (RadioSong current : VanillaDiscs.ALL) {
            for (int draw = 0; draw < DRAWS; draw++) {
                RadioSong picked = selection.next(current, PlayMode.SHUFFLE).orElseThrow();

                Assertions.assertNotEquals(current, picked, "shuffle must not pick " + current.title() + " again after it");
            }
        }
    }
}
