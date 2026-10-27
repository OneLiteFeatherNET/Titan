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
import java.util.Objects;
import java.util.regex.Pattern;
import net.minestom.server.item.Material;

/**
 * A group of titles in the radio overview, declared in {@code radio/songs.yaml}. A missing German
 * or English name takes the other one, so a single text serves both languages.
 *
 * @param id     the id used in {@code songs.yaml}, lower case, unique
 * @param nameDe the German name, may be {@code null} when {@code nameEn} is given
 * @param nameEn the English name, may be {@code null} when {@code nameDe} is given
 * @param icon   the vanilla item that stands for the category in the overview
 * @param order  the position among the declared categories, ascending
 */
record RadioCategory(String id, String nameDe, String nameEn, Material icon, int order) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    RadioCategory {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(icon, "icon must not be null");
        nameDe = blankToNull(nameDe);
        nameEn = blankToNull(nameEn);
        if (nameDe == null && nameEn == null) {
            throw new IllegalArgumentException("a category needs a name in at least one language");
        }
        nameDe = nameDe != null ? nameDe : nameEn;
        nameEn = nameEn != null ? nameEn : nameDe;
    }

    /** Whether {@code id} may name a declared category: lower case, digits, underscore or dash. */
    static boolean isValidId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    /** The ids of the built-in categories, which {@code songs.yaml} must not declare. */
    static boolean isReserved(String id) {
        return RadioCategories.MINECRAFT.id().equals(id) || RadioCategories.OTHER.id().equals(id);
    }

    /** The name in the player's language; anything but German shows English. */
    String name(Locale locale) {
        return Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? this.nameDe : this.nameEn;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
