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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minestom.server.item.Material;

/**
 * The categories of the radio overview: Minecraft first, the declared ones by order, Other last.
 * Only Minecraft is shown without songs; a declared category or Other needs at least one title.
 */
final class RadioCategories {

    static final RadioCategory MINECRAFT = new RadioCategory("minecraft", "Minecraft", "Minecraft", Material.MUSIC_DISC_13, 0);
    static final RadioCategory OTHER = new RadioCategory("other", "Sonstige", "Other", Material.PAPER, 0);

    /**
     * Declared categories beyond this are skipped; with Minecraft and Other the overview fits one
     * window.
     */
    static final int MAX_DECLARED = 20;

    private static final Comparator<RadioCategory> DECLARED_ORDER = Comparator.comparingInt(RadioCategory::order).thenComparing(RadioCategory::id);

    private RadioCategories() {
    }

    /** The categories to show, given the declared ones and the ids that have at least one title. */
    static List<RadioCategory> visible(List<RadioCategory> declared, Set<String> idsWithSongs) {
        List<RadioCategory> visible = new ArrayList<>();
        visible.add(MINECRAFT);
        declared.stream().filter(category -> idsWithSongs.contains(category.id())).sorted(DECLARED_ORDER).forEach(visible::add);
        if (idsWithSongs.contains(OTHER.id())) {
            visible.add(OTHER);
        }
        return List.copyOf(visible);
    }
}
