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

import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioSessionStateTest {

    private static final RadioCategory GAMES = new RadioCategory("games", "Spiele", "Games", Material.EMERALD, 10);

    @DisplayName("A new session starts in the overview on page 1 with no opened category")
    @Test
    void startsInTheOverview() {
        RadioSessionState state = new RadioSessionState();

        Assertions.assertTrue(state.inOverview(), "a new session must show the overview");
        Assertions.assertEquals(1, state.page(), "a new session must be on page 1");
        Assertions.assertNull(state.lastCategory(), "no category was opened yet");
    }

    @DisplayName("Opening a category starts on page 1 of it")
    @Test
    void openingACategoryStartsOnPageOne() {
        RadioSessionState state = new RadioSessionState();
        state.page(3);

        state.openCategory(GAMES);

        Assertions.assertEquals(GAMES, state.openCategory(), "the opened category must be shown");
        Assertions.assertEquals(1, state.page(), "opening a category must reset the page to 1");
    }

    @DisplayName("Changing the page keeps the opened category")
    @Test
    void pagingKeepsTheCategory() {
        RadioSessionState state = new RadioSessionState();
        state.openCategory(GAMES);

        state.page(2);

        Assertions.assertEquals(GAMES, state.openCategory(), "turning a page must not change the category");
        Assertions.assertEquals(2, state.page(), "turning a page must set the page");
    }

    @DisplayName("Going back to the overview keeps the last opened category for Next")
    @Test
    void backToOverviewKeepsTheLastCategory() {
        RadioSessionState state = new RadioSessionState();
        state.openCategory(GAMES);

        state.showOverview();

        Assertions.assertTrue(state.inOverview(), "going back must show the overview");
        Assertions.assertEquals(GAMES, state.lastCategory(), "the last opened category must stay known");
    }

    @DisplayName("Every advance returns a new generation, so an older load result can be told apart")
    @Test
    void advanceCountsUp() {
        RadioSessionState state = new RadioSessionState();
        long first = state.advance();
        long second = state.advance();

        Assertions.assertTrue(second > first, "each advance must return a larger generation, was " + first + " then " + second);
        Assertions.assertEquals(second, state.generation(), "the generation must be the last value advanced to");
    }
}
