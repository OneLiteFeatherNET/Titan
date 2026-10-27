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

import java.util.Locale;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioCategoryTest {

    @DisplayName("A category id may use lower case letters, digits, underscore and dash")
    @Test
    void validIdPatternIsAccepted() {
        Assertions.assertTrue(RadioCategory.isValidId("anime_2-x"), "lower case, digits, underscore and dash must be a valid id");
    }

    @DisplayName("A category id with upper case or a space is rejected")
    @Test
    void idWithUpperCaseIsRejected() {
        Assertions.assertFalse(RadioCategory.isValidId("Anime"), "an upper case letter must not be a valid id");
        Assertions.assertFalse(RadioCategory.isValidId("my games"), "a space must not be a valid id");
    }

    @DisplayName("A category id longer than 32 characters is rejected")
    @Test
    void tooLongIdIsRejected() {
        Assertions.assertFalse(RadioCategory.isValidId("a".repeat(33)), "an id of 33 characters must be rejected");
        Assertions.assertTrue(RadioCategory.isValidId("a".repeat(32)), "an id of 32 characters must be accepted");
    }

    @DisplayName("The ids minecraft and other are reserved")
    @Test
    void builtInIdsAreReserved() {
        Assertions.assertTrue(RadioCategory.isReserved("minecraft"), "minecraft must be reserved");
        Assertions.assertTrue(RadioCategory.isReserved("other"), "other must be reserved");
        Assertions.assertFalse(RadioCategory.isReserved("games"), "games must not be reserved");
    }

    @DisplayName("A category with only a German name shows it in English too")
    @Test
    void missingEnglishNameFallsBackToGerman() {
        RadioCategory category = new RadioCategory("anime", "Anime", null, Material.PAPER, 1);

        Assertions.assertEquals("Anime", category.nameEn(), "the English name must fall back to the German one");
    }

    @DisplayName("A category with only an English name shows it in German too")
    @Test
    void missingGermanNameFallsBackToEnglish() {
        RadioCategory category = new RadioCategory("anime", null, "Anime", Material.PAPER, 1);

        Assertions.assertEquals("Anime", category.nameDe(), "the German name must fall back to the English one");
    }

    @DisplayName("A category without any name is rejected")
    @Test
    void categoryWithoutNameIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadioCategory("anime", null, " ", Material.PAPER, 1), "a category needs a name in at least one language");
    }

    @DisplayName("The name follows the locale of the player")
    @Test
    void nameFollowsTheLocale() {
        RadioCategory category = new RadioCategory("games", "Spiele", "Games", Material.EMERALD, 10);

        Assertions.assertEquals("Spiele", category.name(Locale.GERMAN), "a German player must see the German name");
        Assertions.assertEquals("Games", category.name(Locale.ENGLISH), "an English player must see the English name");
    }

    @DisplayName("The built-in Minecraft category has the reserved id and the first disc as its icon")
    @Test
    void minecraftCategoryIsBuiltIn() {
        Assertions.assertEquals("minecraft", RadioCategories.MINECRAFT.id(), "the built-in category must use the reserved id minecraft");
        Assertions.assertEquals(Material.MUSIC_DISC_13, RadioCategories.MINECRAFT.icon(), "Minecraft must show the disc 13 as its icon");
    }

    @DisplayName("The built-in Other category has the reserved id and the paper icon")
    @Test
    void otherCategoryIsBuiltIn() {
        Assertions.assertEquals("other", RadioCategories.OTHER.id(), "the built-in category must use the reserved id other");
        Assertions.assertEquals(Material.PAPER, RadioCategories.OTHER.icon(), "Other must show paper as its icon");
    }
}
