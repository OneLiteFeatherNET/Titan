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

import java.util.List;
import java.util.Set;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioCategoriesTest {

    private static final RadioCategory GAMES = new RadioCategory("games", "Spiele", "Games", Material.EMERALD, 10);
    private static final RadioCategory CLASSIC = new RadioCategory("classic", "Klassik", "Classic", Material.CLOCK, 10);
    private static final RadioCategory ANIME = new RadioCategory("anime", "Anime", "Anime", Material.FIREWORK_ROCKET, 20);
    private static final RadioCategory HALLOWEEN = new RadioCategory("halloween", "Halloween", "Halloween", Material.JACK_O_LANTERN, 40);

    @DisplayName("Minecraft comes first and Other comes last among the visible categories")
    @Test
    void minecraftFirstAndOtherLast() {
        List<RadioCategory> visible = RadioCategories.visible(List.of(GAMES), Set.of("minecraft", "games", "other"));

        Assertions.assertEquals(List.of(RadioCategories.MINECRAFT, GAMES, RadioCategories.OTHER), visible, "Minecraft must lead and Other must trail");
    }

    @DisplayName("Declared categories are ordered by their order value, then by id")
    @Test
    void declaredOrderIsOrderThenId() {
        List<RadioCategory> visible = RadioCategories.visible(List.of(ANIME, GAMES, CLASSIC), Set.of("games", "classic", "anime"));

        Assertions.assertEquals(List.of(RadioCategories.MINECRAFT, CLASSIC, GAMES, ANIME), visible, "equal order values must sort by id, the order value comes first");
    }

    @DisplayName("A declared category without songs is hidden")
    @Test
    void emptyDeclaredCategoryIsHidden() {
        List<RadioCategory> visible = RadioCategories.visible(List.of(GAMES, HALLOWEEN), Set.of("games"));

        Assertions.assertFalse(visible.contains(HALLOWEEN), "a category without songs must not appear in the overview");
    }

    @DisplayName("Other is hidden while it has no songs")
    @Test
    void otherIsHiddenWithoutSongs() {
        List<RadioCategory> visible = RadioCategories.visible(List.of(GAMES), Set.of("games"));

        Assertions.assertFalse(visible.contains(RadioCategories.OTHER), "Other must stay hidden without songs");
    }

    @DisplayName("Minecraft stays visible even without songs")
    @Test
    void minecraftIsAlwaysVisible() {
        List<RadioCategory> visible = RadioCategories.visible(List.of(), Set.of());

        Assertions.assertEquals(List.of(RadioCategories.MINECRAFT), visible, "Minecraft must always be in the overview");
    }
}
